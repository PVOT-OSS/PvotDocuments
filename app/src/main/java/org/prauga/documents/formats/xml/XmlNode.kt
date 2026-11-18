package org.prauga.documents.formats.xml

/**
 * A small in-memory XML element, for parts that are read by looking things up rather than in one
 * pass: a slide inherits positions, text styles and colors from its layout and master, which is
 * far simpler to resolve over trees than over streams. Text keeps its place among the children
 * as [TEXT] nodes, so mixed content (ODF paragraphs with spans) reads in order.
 */
internal class XmlNode(val name: String, private val attributes: Map<String, String>) {
    val children = ArrayList<XmlNode>(2)
    var text: String = ""
        private set

    fun attr(name: String): String? = attributes[name]

    fun child(name: String): XmlNode? = children.firstOrNull { it.name == name }

    fun children(name: String): List<XmlNode> = children.filter { it.name == name }

    /** The first element down the [path] of child names, or null. */
    fun at(vararg path: String): XmlNode? {
        var node: XmlNode = this
        for (name in path) node = node.child(name) ?: return null
        return node
    }

    /** The first descendant named [name], searching depth-first. */
    fun find(name: String): XmlNode? {
        for (c in children) {
            if (c.name == name) return c
            c.find(name)?.let { return it }
        }
        return null
    }

    /** All the text inside, in order. */
    fun allText(): String = if (name == TEXT) text else children.joinToString("") { it.allText() }

    companion object {
        const val TEXT = "#text"

        fun text(value: String) = XmlNode(TEXT, emptyMap()).apply { text = value }
    }
}
