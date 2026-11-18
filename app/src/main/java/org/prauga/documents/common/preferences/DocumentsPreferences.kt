package org.prauga.documents.common.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.prauga.documents.common.coroutines.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentsPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val preferences by lazy { context.getSharedPreferences(FileName, Context.MODE_PRIVATE) }

    suspend fun get(): SharedPreferences = withContext(io) { preferences }

    private companion object {
        const val FileName = "documents_preferences"
    }
}
