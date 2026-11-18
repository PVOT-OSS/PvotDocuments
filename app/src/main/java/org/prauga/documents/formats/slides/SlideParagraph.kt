package org.prauga.documents.formats.slides

import org.prauga.documents.formats.richtext.ParagraphAlign
import org.prauga.documents.formats.richtext.TextRun

/** A paragraph of slide text: [bullet] is the marker drawn before it (already worked out, "•" or
 * "3."), [level] its indent level from 0, and [spaceBeforePt] the gap above it. */
data class SlideParagraph(
    val runs: List<TextRun>,
    val align: ParagraphAlign = ParagraphAlign.Start,
    val bullet: String? = null,
    val level: Int = 0,
    val spaceBeforePt: Float = 0f,
    /** Line height as a multiple of the font's normal line height. */
    val lineSpacing: Float = 1f,
    /** Size, in points, of an empty paragraph's line, so blank lines keep their height. */
    val emptySizePt: Float? = null,
    /** Where the text starts, in points from the shape's inner left edge, and where the first
     * line (and its bullet) starts relative to that: negative for a hanging bullet. */
    val marginLeftPt: Float = 0f,
    val indentPt: Float = 0f,
)
