package org.prauga.documents.viewer.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.prauga.documents.common.coroutines.ApplicationScope
import org.prauga.documents.common.coroutines.IoDispatcher
import org.prauga.documents.common.preferences.DocumentsPreferences
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Where reading stopped in each document, for the last [MaxReadingPositions] documents read. */
@Singleton
class ReadingPositionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: DocumentsPreferences,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val writes = Mutex()

    /**
     * What a document's reading position is remembered under: its name and size, rather than its
     * URI. The same file reaches the viewer under different URIs — each "Open with" saves a fresh
     * copy, and a folder can be re-added — but it keeps its name and size. Falls back to the URI
     * when the size can't be read.
     */
    suspend fun key(uri: Uri, name: String): String = withContext(io) {
        val size = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
            }
        }.onFailure { Timber.w("Could not read a document size: %s", it.javaClass.name) }.getOrNull()
        if (size != null && size > 0) "$name|$size" else uri.toString()
    }

    suspend fun position(key: String): ReadingPosition? =
        decode(preferences.get().getString(KeyReadingPositions, null)).firstOrNull { it.first == key }?.second

    fun save(key: String, position: ReadingPosition) {
        scope.launch {
            writes.withLock {
                val stored = preferences.get()
                val existing = decode(stored.getString(KeyReadingPositions, null))
                val updated = (listOf(key to position) + existing.filterNot { it.first == key })
                    .take(MaxReadingPositions)
                stored.edit { putString(KeyReadingPositions, encode(updated)) }
            }
        }
    }

    private fun encode(positions: List<Pair<String, ReadingPosition>>): String {
        val json = JSONArray()
        positions.forEach { (key, position) ->
            json.put(
                JSONObject()
                    .put("key", key)
                    .put("page", position.page)
                    .put("offset", position.offsetFraction.toDouble())
            )
        }
        return json.toString()
    }

    private fun decode(json: String?): List<Pair<String, ReadingPosition>> {
        if (json == null) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { i ->
                val item = array.getJSONObject(i)
                item.getString("key") to ReadingPosition(item.getInt("page"), item.optDouble("offset", 0.0).toFloat())
            }
        }.onFailure { Timber.w("Dropping unreadable reading positions: %s", it.javaClass.name) }
            .getOrDefault(emptyList())
    }

    private companion object {
        const val KeyReadingPositions = "reading_positions"
        const val MaxReadingPositions = 100
    }
}
