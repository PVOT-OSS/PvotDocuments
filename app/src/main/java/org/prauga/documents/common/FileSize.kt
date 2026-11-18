// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.common

import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun formatFileSize(bytes: Long): String = Formatter.formatShortFileSize(LocalContext.current, bytes)
