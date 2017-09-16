package fr.nihilus.music.media.mock

import android.database.Cursor
import android.net.Uri
import android.support.v4.util.ArrayMap
import android.test.mock.MockContentProvider

/**
 * A mock [android.content.ContentProvider] used for testing.
 * When queried, it returns a cursor associated with an Uri with the [registerQueryResult] method.
 *
 * All methods others than [query] throw an [UnsupportedOperationException].
 */
class MockCursorProvider : MockContentProvider() {
    private val queryMap = ArrayMap<Uri, Cursor?>()

    override fun query(uri: Uri?, projection: Array<out String>?, selection: String?,
                       selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        return queryMap[uri]
    }

    /**
     * Associate a Cursor to an Uri,
     * so that it will be returned when the provider is queried for that Uri.
     */
    fun registerQueryResult(uri: Uri, cursor: Cursor?) {
        queryMap.put(uri, cursor)
    }
}