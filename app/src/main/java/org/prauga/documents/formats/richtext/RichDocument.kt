package org.prauga.documents.formats.richtext

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import timber.log.Timber
import java.io.Closeable
import java.io.File
import java.util.zip.ZipFile

/**
 * A parsed word-processor document, and the package it came from — kept open while it's viewed
 * so images are read only when they scroll into view, rather than all held in memory at once.
 * [close] deletes the private copy.
 */
class RichDocument(
    val blocks: List<DocBlock>,
    private val packageFile: File,
    /** The document's default font size in points; runs at this size leave [RunStyle.sizePt]
     * null, so a heading without a size of its own still looks like a heading. */
    val defaultSizePt: Float,
) : Closeable {
    private val zip by lazy { ZipFile(packageFile) }

    /** The picture at [path], decoded at no more than [maxWidthPx] wide. Null if it's missing or
     * not a format Android can decode (EMF and WMF drawings, for one). */
    @Synchronized
    fun image(path: String, maxWidthPx: Int): Bitmap? = runCatching {
        val entry = zip.getEntry(path) ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        zip.getInputStream(entry).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxWidthPx) sample *= 2
        zip.getInputStream(entry).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }.getOrNull()

    override fun close() {
        runCatching { zip.close() }.onFailure { Timber.w("Could not close a document package: %s", it.javaClass.name) }
        packageFile.delete()
    }
}

/** Points per EMU, the unit Office drawings are sized in (914,400 to the inch). */
internal const val PointsPerEmu = 72f / 914_400f

/** A list counter as a label in [format]: "decimal", "lowerLetter", "upperRoman" and so on, the
 * names DOCX uses (ODT's "1", "a", "I" are mapped to them). */
internal fun listNumber(n: Int, format: String): String = when (format) {
    "lowerLetter" -> letters(n).lowercase()
    "upperLetter" -> letters(n)
    "lowerRoman" -> roman(n).lowercase()
    "upperRoman" -> roman(n)
    "decimalZero" -> if (n < 10) "0$n" else n.toString()
    "none" -> ""
    else -> n.toString()
}

private fun letters(n: Int): String {
    val out = StringBuilder()
    var v = n
    while (v > 0) {
        val rem = (v - 1) % 26
        out.append('A' + rem)
        v = (v - 1) / 26
    }
    return out.reverse().toString()
}

private fun roman(n: Int): String {
    if (n <= 0 || n >= 4000) return n.toString()
    val values = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
    val symbols = arrayOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")
    val out = StringBuilder()
    var v = n
    for (i in values.indices) while (v >= values[i]) { out.append(symbols[i]); v -= values[i] }
    return out.toString()
}

/** A hex color like "FF0000" or "#ff0000" as opaque ARGB, or null for "auto" and anything else. */
internal fun hexColor(value: String?): Int? {
    val hex = value?.removePrefix("#") ?: return null
    if (hex.length != 6) return null
    return hex.toIntOrNull(16)?.let { it or 0xFF000000.toInt() }
}
