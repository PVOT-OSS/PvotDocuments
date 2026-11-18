// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.formats

import java.io.File

/** A private copy of test resource [name]: the parsers keep, and later delete, the file they read. */
fun fixture(name: String): File {
    val source = checkNotNull(Thread.currentThread().contextClassLoader?.getResourceAsStream("fixtures/$name")) {
        "missing fixture $name"
    }
    val copy = File.createTempFile("fixture", name.substringAfterLast('.', "").let { ".$it" })
    source.use { input -> copy.outputStream().use { input.copyTo(it) } }
    return copy
}
