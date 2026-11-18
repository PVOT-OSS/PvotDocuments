package org.prauga.documents.viewer.pdf

/** One entry of the document's outline (its bookmarks), flattened in reading order; [depth] is 0
 * for top-level entries. */
data class OutlineEntry(val title: String, val pageIndex: Int, val depth: Int)
