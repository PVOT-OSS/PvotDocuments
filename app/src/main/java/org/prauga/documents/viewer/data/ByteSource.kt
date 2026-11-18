// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.viewer.data

import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer

/** A file read at any offset without holding it in memory; [close] also deletes [ownedCopy]. */
class ByteSource(
    private val descriptor: ParcelFileDescriptor,
    private val io: CoroutineDispatcher,
    private val ownedCopy: File? = null,
) : Closeable {
    val size: Long = descriptor.statSize
    private val channel = FileInputStream(descriptor.fileDescriptor).channel

    suspend fun read(offset: Long, length: Int): ByteArray = withContext(io) {
        synchronized(this@ByteSource) {
            val count = minOf(length.toLong(), size - offset).coerceAtLeast(0).toInt()
            val buffer = ByteBuffer.allocate(count)
            var position = offset
            while (buffer.hasRemaining()) {
                val read = channel.read(buffer, position)
                if (read < 0) break
                position += read
            }
            buffer.array().copyOf(buffer.position())
        }
    }

    override fun close() {
        channel.close()
        descriptor.close()
        ownedCopy?.delete()
    }
}
