package fr.nihilus.mymusic.media

import android.content.ContentResolver
import android.content.Context
import android.database.MatrixCursor
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import android.support.test.runner.AndroidJUnit4
import android.support.v4.media.MediaMetadataCompat
import android.test.mock.MockContentResolver
import fr.nihilus.mymusic.media.mock.MockCursorProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class MediaDaoTest {

    private lateinit var testSubject: MediaDao
    private lateinit var cursor: MatrixCursor

    @Before
    fun setUp() {
        cursor = getMockCursor()
        val mockProvider = MockCursorProvider()
        mockProvider.registerQueryResult(Media.EXTERNAL_CONTENT_URI, cursor)

        val context = mock(Context::class.java)
        val mockResolver = MockContentResolver()
        `when`<ContentResolver>(context.contentResolver).thenReturn(mockResolver)
        mockResolver.addProvider(MediaStore.AUTHORITY, mockProvider)

        testSubject = MediaDao(context)
    }

    @Test
    fun allTracks_cursorToMetadata() {
        testSubject.getAllTracks().test()
                .awaitCount(1)
                .assertValue { metadataList ->
                    metadataHasValues(metadataList[0], "1", "Title", "Album",
                            "Artist", 123L, 1L, 1L, artUriOf(1))
                            && metadataHasValues(metadataList[1], "2", "Amerika", "Reise Reise",
                            "Rammstein", 3046L, 1L, 6L, artUriOf(2))
                }.dispose()
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

private fun metadataHasValues(meta: MediaMetadataCompat, mediaId: String, title: String,
                              album: String, artist: String, duration: Long, discNo: Long,
                              trackNo: Long, artUri: String): Boolean {
    return mediaId == meta.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
            && title == meta.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
            && album == meta.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)
            && artist == meta.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
            && duration == meta.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
            && discNo == meta.getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER)
            && trackNo == meta.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER)
            && artUri == meta.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
}
