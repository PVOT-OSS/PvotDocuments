package org.prauga.documents.common

import kotlinx.coroutines.flow.SharingStarted

/** Keeps a screen's state flowing across a configuration change without holding it forever. */
val WhileScreenSubscribed: SharingStarted = SharingStarted.WhileSubscribed(5_000L)
