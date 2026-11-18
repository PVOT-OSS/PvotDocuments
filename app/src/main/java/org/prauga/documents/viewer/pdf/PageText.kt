package org.prauga.documents.viewer.pdf

/** A page's text as Pdfium extracted it: [text] has one UTF-16 unit per character index, and
 * [boxes] holds that character's box (null for characters Pdfium generated, like the spaces and
 * line breaks it inserts between runs, which have no box of their own). */
internal class PageText(val text: String, val boxes: List<PageRect?>)
