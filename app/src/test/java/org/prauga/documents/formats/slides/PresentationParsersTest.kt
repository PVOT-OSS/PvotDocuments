// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.formats.slides

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.documents.formats.fixture
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PresentationParsersTest {

    @Test
    fun readsPowerPointSlidesAndTheirText() = check(parsePptx(fixture("slides.pptx")))

    @Test
    fun readsOpenDocumentPresentationsTheSameWay() = check(parseOdp(fixture("slides.odp")))

    private fun check(presentation: Presentation) = presentation.use {
        assertEquals(1, it.slides.size)
        assertTrue(it.width > 0f && it.height > it.width)
        val text = it.slides.single().shapes
            .filterIsInstance<SlideShape.Shape>()
            .mapNotNull { shape -> shape.text }
            .flatMap { t -> t.paragraphs }
            .joinToString(" ") { p -> p.runs.joinToString("") { run -> run.text } }
        assertTrue(text, "Quarterly report" in text)
        assertTrue(text, "One" in text && "Two" in text)
    }
}
