// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.viewer.text

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.documents.R
import org.prauga.documents.common.formatFileSize
import org.prauga.documents.viewer.data.ByteSource

/** A file no viewer understands, as offset, bytes in hex and the printable ones as text, read a row
 * at a time as it scrolls into view. */
@Composable
internal fun BytesView(source: ByteSource, mimeType: String, topPadding: Dp, modifier: Modifier = Modifier) {
    val rows = ((source.size + BytesPerRow - 1) / BytesPerRow).toInt()
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = PvotSpacing.L, end = PvotSpacing.L, top = topPadding + PvotSpacing.L, bottom = PvotSpacing.L),
    ) {
        item {
            Text(
                text = stringResource(
                    R.string.viewer_bytes_summary,
                    formatFileSize(source.size),
                    mimeType.ifEmpty { stringResource(R.string.viewer_type_unknown) },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(bottom = PvotSpacing.M),
            )
        }
        items(rows) { row -> BytesRow(source, row) }
    }
}

@Composable
private fun BytesRow(source: ByteSource, row: Int) {
    val offset = row.toLong() * BytesPerRow
    val bytes by produceState<ByteArray?>(null, source, row) { value = source.read(offset, BytesPerRow) }
    Text(
        text = bytes?.let { formatRow(offset, it) } ?: "%08X".format(offset),
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

private fun formatRow(offset: Long, bytes: ByteArray): String {
    val hex = bytes.joinToString(" ") { "%02X".format(it) }.padEnd(BytesPerRow * 3 - 1)
    val printable = bytes.joinToString("") { b -> (b.toInt() and 0xFF).let { if (it in 0x20..0x7E) it.toChar().toString() else "." } }
    return "%08X  %s  %s".format(offset, hex, printable)
}

// Eight, not the usual sixteen, so a row fits a phone's width in portrait.
private const val BytesPerRow = 8
