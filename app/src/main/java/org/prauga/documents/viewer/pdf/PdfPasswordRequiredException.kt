package org.prauga.documents.viewer.pdf

/** Thrown by [PdfiumDocument.open] when the document is encrypted and [password] was missing or
 * wrong. */
internal class PdfPasswordRequiredException(val passwordWasGiven: Boolean) : Exception()
