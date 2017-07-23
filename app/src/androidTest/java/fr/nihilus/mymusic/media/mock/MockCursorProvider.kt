package fr.nihilus.mymusic.media.mock

import android.database.Cursor
import android.net.Uri
import android.test.mock.MockContentProvider

/**
 * A mock ContentProvider used for testing.
 * When queried for any URI it returns the cursor provided through the constructor.
 *
 * All methods others than query throw an UnsupportedOperationException.
 */
class MockCursorProvider(private val cursor: Cursor?) : MockContentProvider() {

    override fun query(uri: Uri?, projection: Array<out String>?, selection: String?,
                       selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = cursor
}