package org.prauga.documents.common

import org.prauga.documents.library.data.DocumentEntry

enum class DocumentSort {
    Name,
    DateNewest,
    SizeLargest
}

fun <T> List<T>.sortedByDocument(sort: DocumentSort, document: (T) -> DocumentEntry): List<T> =
    when (sort) {
        DocumentSort.Name -> sortedBy { document(it).name.lowercase() }
        DocumentSort.DateNewest -> sortedByDescending { document(it).lastModified }
        DocumentSort.SizeLargest -> sortedByDescending { document(it).sizeBytes }
    }
