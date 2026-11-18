package org.prauga.documents.formats.slides

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import timber.log.Timber
import java.io.Closeable
import java.io.File
import java.util.zip.ZipFile

/**
 * A parsed presentation and the package it came from, kept open while it's viewed so pictures
 * are read only as their slides come into view. [close] deletes the private copy.
 */
class Presentation(
    val slides: List<Slide>,
    /** Slide size in points. */
    val width: Float,
    val height: Float,
    private val packageFile: File,
) : Closeable {
    private val zip by lazy { ZipFile(packageFile) }

    /** The picture at [path], decoded at no more than [maxWidthPx] wide; null if it's missing or
     * a format Android can't decode (such as EMF and WMF drawings). */
    @Synchronized
    fun image(path: String, maxWidthPx: Int): Bitmap? = runCatching {
        val entry = zip.getEntry(path) ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        zip.getInputStream(entry).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxWidthPx.coerceAtLeast(1)) sample *= 2
        zip.getInputStream(entry).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }.getOrNull()

    override fun close() {
        runCatching { zip.close() }.onFailure { Timber.w("Could not close a document package: %s", it.javaClass.name) }
        packageFile.delete()
    }
}
