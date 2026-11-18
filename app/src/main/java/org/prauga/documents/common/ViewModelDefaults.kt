// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.common

import kotlinx.coroutines.flow.SharingStarted

/** Keeps a screen's state flowing across a configuration change without holding it forever. */
val WhileScreenSubscribed: SharingStarted = SharingStarted.WhileSubscribed(5_000L)
