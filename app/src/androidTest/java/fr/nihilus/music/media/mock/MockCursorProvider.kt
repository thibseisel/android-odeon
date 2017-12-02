package fr.nihilus.music.media.mock

import android.database.Cursor
import android.net.Uri
import android.support.v4.util.ArrayMap
import android.test.mock.MockContentProvider

/**
 * A mock [android.content.ContentProvider] used for testing.
 * When queried, it returns cursors associated with an Uri with the [registerQueryResult] method.
 *
 * When done testing, you may [reset] this provider so that its instance can be reused
 * for other tests, removing the need to create new instances for each test.
 *
 * All methods others than [query] throw an [UnsupportedOperationException].
 */
class MockCursorProvider : MockContentProvider() {
    private val queryMap = ArrayMap<Uri, Cursor?>()

    override fun query(uri: Uri?, projection: Array<out String>?, selection: String?,
                       selectionArgs: Array<out String>?, sortOrder: String?) =
            queryMap[uri]

    /**
     * Associate a Cursor to an Uri,
     * so that it will be returned when the provider is queried for that Uri.
     */
    fun registerQueryResult(uri: Uri, cursor: Cursor?) {
        queryMap.put(uri, cursor)
    }

    /**
     * Clear all cursors associated to an Uri, resetting this provider to its initial state.
     */
    fun reset() {
        queryMap.clear()
    }
}