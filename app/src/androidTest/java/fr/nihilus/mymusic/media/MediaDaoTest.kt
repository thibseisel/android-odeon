package fr.nihilus.mymusic.media

import android.app.Application
import android.content.ContentResolver
import android.database.MatrixCursor
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import android.support.test.runner.AndroidJUnit4
import android.support.v4.media.MediaMetadataCompat
import android.test.mock.MockContentResolver
import fr.nihilus.mymusic.media.mock.MockCursorProvider
import io.reactivex.observers.TestObserver
import org.hamcrest.Matchers.hasSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class MediaDaoTest {

    private var testSubject: MediaDao? = null
    private var cursor: MatrixCursor? = null

    @Before
    fun setUp() {
        cursor = getMockCursor()
        val mockProvider = MockCursorProvider()
        mockProvider.registerQueryResult(Media.EXTERNAL_CONTENT_URI, cursor)

        val app = mock(Application::class.java)
        val mockResolver = MockContentResolver()
        `when`<ContentResolver>(app.contentResolver).thenReturn(mockResolver)
        mockResolver.addProvider(MediaStore.AUTHORITY, mockProvider)

        testSubject = MediaDao(app)
    }

    @Test
    fun allTracks_cursorToMetadata() {
        val data = testSubject!!.getAllTracks().firstElement().blockingGet()

        assertThat(data, hasSize<Any>(VALUES.size))
        assertMetadataHas(data[0], "1", "Title", "Album", "Artist", 123L, 1L, 1L, artUriOf(1))
        assertMetadataHas(data[1], "2", "Amerika", "Reise Reise", "Rammstein", 3046L, 1L, 6L, artUriOf(2))
    }

    @Test
    fun allTracks_reloadedWhenContentChanged() {
        val observer = TestObserver<List<MediaMetadataCompat>>()
        testSubject!!.getAllTracks().subscribe(observer)
    }

    private fun assertMetadataHas(meta: MediaMetadataCompat, mediaId: String, title: String,
                                  album: String, artist: String, duration: Long, discNo: Long,
                                  trackNo: Long, artUri: String) {
        assertEquals(mediaId, meta.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID))
        assertEquals(title, meta.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
        assertEquals(album, meta.getString(MediaMetadataCompat.METADATA_KEY_ALBUM))
        assertEquals(artist, meta.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
        assertEquals(duration, meta.getLong(MediaMetadataCompat.METADATA_KEY_DURATION))
        assertEquals(discNo, meta.getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER))
        assertEquals(trackNo, meta.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER))
        assertEquals(artUri, meta.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI))
    }
}

// Test materials
private fun artUriOf(musicId: Long) = "content://media/external/audio/albumart/$musicId"

private val VALUES = arrayOf(
        arrayOf(1L, "Title", "Album", "Artist", 123, 101, "TitleKey", "AlbumKey", 1L, 1L, ""),
        arrayOf(2L, "Amerika", "Reise Reise", "Rammstein", 3046, 106, "Amerika", "ReiseReise", 2L, 2L, ""),
        arrayOf(42L, "Fever", "Fever", "Bullet For My Valentine", 2567, 101, "Fever", "Fever", 3L, 3L, "")
)

private fun getMockCursor(): MatrixCursor {
    val columns = arrayOf(BaseColumns._ID, Media.TITLE, Media.ALBUM, Media.ARTIST, Media.DURATION,
            Media.TRACK, Media.TITLE_KEY, Media.ALBUM_KEY,
            Media.ALBUM_ID, Media.ARTIST_ID, Media.DATA)

    val cursor = MatrixCursor(columns)
    for (row in VALUES) {
        cursor.addRow(row)
    }

    return cursor
}
