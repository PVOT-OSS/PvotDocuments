// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.formats.epub

import org.prauga.documents.formats.richtext.DocBlock
import org.prauga.documents.formats.richtext.DocImage
import org.prauga.documents.formats.richtext.DocPageBreak
import org.prauga.documents.formats.richtext.DocParagraph
import org.prauga.documents.formats.richtext.DocTable
import org.prauga.documents.formats.richtext.DocTableCell
import org.prauga.documents.formats.richtext.ListMarker
import org.prauga.documents.formats.richtext.ParagraphAlign
import org.prauga.documents.formats.richtext.RichDocument
import org.prauga.documents.formats.richtext.RunStyle
import org.prauga.documents.formats.richtext.TextPosition
import org.prauga.documents.formats.richtext.TextRun
import org.prauga.documents.formats.richtext.mergeRuns
import org.prauga.documents.formats.xml.XmlNode
import org.prauga.documents.formats.xml.parseXmlTree
import java.io.File
import java.net.URLDecoder
import java.util.zip.ZipFile

/**
 * An EPUB 2 or 3 book, its chapters in reading order with a break between them. Styling is the
 * book's markup (headings, emphasis, lists, tables, pictures), not its CSS; fixed-layout books
 * read as flowing text.
 *
 * [file] must be a private copy: the [RichDocument] keeps it to read pictures from, and deletes it
 * when closed.
 */
fun parseEpub(file: File): RichDocument {
    val blocks = ZipFile(file).use { zip ->
        fun read(path: String): XmlNode? = zip.getEntry(path)?.let { entry ->
            val markup = zip.getInputStream(entry).use { it.readBytes().toString(Charsets.UTF_8) }
            parseXmlTree(xmlSafe(markup).byteInputStream())
        }
        val opfPath = read("META-INF/container.xml")?.find("rootfile")?.attr("full-path")
            ?: error("Not an EPUB book")
        val opf = read(opfPath) ?: error("Not an EPUB book")
        val opfDir = opfPath.substringBeforeLast('/', "")
        val manifest = opf.find("manifest")?.children("item").orEmpty()
            .mapNotNull { item -> item.attr("id")?.let { id -> id to resolve(opfDir, item.attr("href").orEmpty()) } }
            .toMap()
        val chapters = opf.find("spine")?.children("itemref").orEmpty()
            .filter { it.attr("linear") != "no" }
            .mapNotNull { manifest[it.attr("idref")] }
        val out = ArrayList<DocBlock>()
        for (path in chapters) {
            val body = read(path)?.find("body") ?: continue
            if (out.isNotEmpty()) out.add(DocPageBreak)
            out.addAll(HtmlBlocks(path.substringBeforeLast('/', "")).apply { read(body, RunStyle.Default) }.finish())
        }
        out
    }
    return RichDocument(blocks, file, EpubDefaultSizePt)
}

private const val EpubDefaultSizePt = 12f

private val Bullets = listOf("•", "◦", "▪")

/** The chapter markup as an XML parser takes it: HTML's named entities written out, the ones XML
 * knows left alone, and any other stray "&" escaped. */
private fun xmlSafe(markup: String): String = EntityRegex.replace(markup) { match ->
    val name = match.groupValues[1]
    when {
        name in XmlEntities -> match.value
        name.startsWith("#") -> match.value
        else -> HtmlEntities[name]?.let { "&#$it;" } ?: "&amp;$name;"
    }
}

private val EntityRegex = Regex("&(#?[A-Za-z0-9]+);")
private val XmlEntities = setOf("amp", "lt", "gt", "quot", "apos")
private val HtmlEntities = mapOf(
    "nbsp" to 160, "shy" to 173, "copy" to 169, "reg" to 174, "trade" to 8482, "deg" to 176,
    "middot" to 183, "bull" to 8226, "hellip" to 8230, "ndash" to 8211, "mdash" to 8212,
    "lsquo" to 8216, "rsquo" to 8217, "sbquo" to 8218, "ldquo" to 8220, "rdquo" to 8221,
    "bdquo" to 8222, "laquo" to 171, "raquo" to 187, "times" to 215, "divide" to 247,
    "para" to 182, "sect" to 167, "dagger" to 8224, "Dagger" to 8225, "prime" to 8242,
    "eacute" to 233, "egrave" to 232, "aacute" to 225, "agrave" to 224, "ouml" to 246,
    "uuml" to 252, "auml" to 228, "ccedil" to 231, "ntilde" to 241, "szlig" to 223,
    "thinsp" to 8201, "ensp" to 8194, "emsp" to 8195, "zwnj" to 8204, "zwj" to 8205,
)

/** [href] relative to the directory [base], as a path inside the book; fragments are dropped. */
private fun resolve(base: String, href: String): String {
    val target = URLDecoder.decode(href.substringBefore('#').substringBefore('?'), "UTF-8")
    val parts = ArrayList<String>()
    if (!target.startsWith("/")) parts.addAll(base.split('/').filter { it.isNotEmpty() })
    for (part in target.split('/')) {
        when (part) {
            "", "." -> Unit
            ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex)
            else -> parts.add(part)
        }
    }
    return parts.joinToString("/")
}

/** XHTML into blocks: block elements end the paragraph being built, inline ones change the style
 * of the text inside them. */
private class HtmlBlocks(private val baseDir: String) {
    private val blocks = ArrayList<DocBlock>()
    private val runs = ArrayList<TextRun>()
    private val lists = ArrayList<ListState>()
    private var heading: Int? = null
    private var marker: ListMarker? = null
    private var align = ParagraphAlign.Start

    fun finish(): List<DocBlock> {
        flush()
        return blocks
    }

    fun read(node: XmlNode, style: RunStyle, preformatted: Boolean = false) {
        for (child in node.children) {
            if (child.name == XmlNode.TEXT) {
                val text = if (preformatted) child.text else child.text.replace(Whitespace, " ")
                if (text.isNotEmpty()) runs.add(TextRun(text, style))
                continue
            }
            when (val tag = child.name.substringAfter(':').lowercase()) {
                "script", "style", "head", "title" -> Unit
                "br" -> runs.add(TextRun("\n", style))
                "h1", "h2", "h3", "h4", "h5", "h6" -> block(child) {
                    heading = tag[1].digitToInt()
                    read(child, style.copy(bold = true))
                }
                "pre" -> block(child) { read(child, style.copy(monospace = true), preformatted = true) }
                "ul", "ol" -> {
                    flush()
                    val start = child.attr("start")?.toIntOrNull() ?: 1
                    lists.add(ListState(ordered = tag == "ol", next = start))
                    read(child, style)
                    lists.removeAt(lists.lastIndex)
                    flush()
                }
                "li" -> block(child) {
                    val list = lists.lastOrNull()
                    val level = (lists.size - 1).coerceAtLeast(0)
                    marker = if (list?.ordered == true) {
                        ListMarker("${list.next++}.", level)
                    } else {
                        ListMarker(Bullets[level % Bullets.size], level)
                    }
                    read(child, style)
                }
                "table" -> {
                    flush()
                    blocks.add(table(child, style))
                }
                "img" -> image(child.attr("src"), child.attr("alt"))
                "image" -> image(child.attr("xlink:href") ?: child.attr("href"), null)
                "hr" -> flush()
                "p", "div", "section", "article", "header", "footer", "aside", "main", "nav",
                "figure", "figcaption", "blockquote", "address", "dl", "dt", "dd", "center",
                "body" -> block(child) { read(child, style) }
                "b", "strong" -> read(child, style.copy(bold = true))
                "i", "em", "cite", "var", "dfn" -> read(child, style.copy(italic = true))
                "u", "ins" -> read(child, style.copy(underline = true))
                "s", "strike", "del" -> read(child, style.copy(strike = true))
                "code", "tt", "kbd", "samp" -> read(child, style.copy(monospace = true), preformatted)
                "sup" -> read(child, style.copy(position = TextPosition.Superscript))
                "sub" -> read(child, style.copy(position = TextPosition.Subscript))
                "a" -> {
                    val href = child.attr("href")?.takeIf { LinkSchemes.any { scheme -> it.startsWith(scheme) } }
                    read(child, if (href != null) style.copy(link = href, underline = true) else style)
                }
                else -> read(child, style, preformatted)
            }
        }
    }

    private inline fun block(node: XmlNode, content: () -> Unit) {
        flush()
        align = alignOf(node)
        content()
        flush()
    }

    private fun alignOf(node: XmlNode): ParagraphAlign {
        val css = node.attr("style").orEmpty().replace(" ", "").lowercase()
        return when {
            node.attr("align") == "center" || "text-align:center" in css -> ParagraphAlign.Center
            node.attr("align") == "right" || "text-align:right" in css -> ParagraphAlign.End
            "text-align:justify" in css -> ParagraphAlign.Justify
            else -> ParagraphAlign.Start
        }
    }

    private fun image(src: String?, alt: String?) {
        if (src.isNullOrBlank() || src.startsWith("data:")) return
        flush()
        blocks.add(DocImage(resolve(baseDir, src), null, null, alt?.takeIf { it.isNotBlank() }))
    }

    private fun table(node: XmlNode, style: RunStyle): DocTable {
        val rows = ArrayList<List<DocTableCell>>()
        fun collect(parent: XmlNode) {
            for (child in parent.children) {
                when (child.name.substringAfter(':').lowercase()) {
                    "thead", "tbody", "tfoot" -> collect(child)
                    "tr" -> rows.add(
                        child.children
                            .filter { it.name.lowercase() == "td" || it.name.lowercase() == "th" }
                            .map { cell ->
                                val cellStyle = if (cell.name.lowercase() == "th") style.copy(bold = true) else style
                                val content = HtmlBlocks(baseDir).apply { read(cell, cellStyle) }.finish()
                                DocTableCell(content, cell.attr("colspan")?.toIntOrNull()?.coerceAtLeast(1) ?: 1)
                            }
                    )
                }
            }
        }
        collect(node)
        return DocTable(rows)
    }

    private fun flush() {
        if (runs.isEmpty()) return
        val trimmed = trimRuns(runs)
        runs.clear()
        if (trimmed.isEmpty() && marker == null) return
        blocks.add(DocParagraph(mergeRuns(trimmed), heading, align, marker, marker?.level ?: 0))
        heading = null
        marker = null
    }

    private fun trimRuns(source: List<TextRun>): List<TextRun> {
        val list = source.toMutableList()
        while (list.isNotEmpty() && list.first().text.isBlank()) list.removeAt(0)
        while (list.isNotEmpty() && list.last().text.isBlank()) list.removeAt(list.lastIndex)
        if (list.isEmpty()) return list
        list[0] = list[0].let { TextRun(it.text.trimStart(), it.style) }
        list[list.lastIndex] = list.last().let { TextRun(it.text.trimEnd(), it.style) }
        return list
    }

    private class ListState(val ordered: Boolean, var next: Int)

    private companion object {
        val Whitespace = Regex("\\s+")
        val LinkSchemes = listOf("http://", "https://", "mailto:")
    }
}
