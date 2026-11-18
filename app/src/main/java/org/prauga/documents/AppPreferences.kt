package org.prauga.documents

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

/** The app's settings, kept in SharedPreferences and observable from Compose. */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Where reading stopped in the document [key] names (see `readingKey` in the viewer), if
     * it has been opened before. */
    fun readingPosition(key: String): ReadingPosition? {
        return decodeReadingPositions(prefs.getString(KEY_READING_POSITIONS, null))
            .firstOrNull { it.first == key }?.second
    }

    /** Remembers [position] for the document [key] names, most recent first; only the last
     * [MaxReadingPositions] documents are kept, so the list doesn't grow forever. */
    fun saveReadingPosition(key: String, position: ReadingPosition) {
        val existing = decodeReadingPositions(prefs.getString(KEY_READING_POSITIONS, null))
        val updated = (listOf(key to position) + existing.filterNot { it.first == key })
            .take(MaxReadingPositions)
        val json = JSONArray()
        updated.forEach { (entryKey, pos) ->
            json.put(JSONObject().put("key", entryKey).put("page", pos.page).put("offset", pos.offsetFraction.toDouble()))
        }
        prefs.edit { putString(KEY_READING_POSITIONS, json.toString()) }
    }

    private companion object {
        const val PREFS_NAME = "documents_preferences"
        const val KEY_READING_POSITIONS = "reading_positions"
        const val MaxReadingPositions = 100

        fun decodeReadingPositions(json: String?): List<Pair<String, ReadingPosition>> =
            runCatching {
                val array = JSONArray(json ?: return emptyList())
                List(array.length()) { i ->
                    val o = array.getJSONObject(i)
                    o.getString("key") to ReadingPosition(o.getInt("page"), o.optDouble("offset", 0.0).toFloat())
                }
            }.onFailure { Timber.w("Dropping unreadable reading positions: %s", it.javaClass.name) }.getOrDefault(emptyList())
    }
}

/** A place in a PDF: the zero-based [page] at the top of the screen, and how far down it the screen
 * starts, as a fraction of the page's height. */
data class ReadingPosition(val page: Int, val offsetFraction: Float)
