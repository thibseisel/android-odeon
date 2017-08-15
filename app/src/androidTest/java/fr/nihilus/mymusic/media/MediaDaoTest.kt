package fr.nihilus.mymusic.media

import android.content.ContentResolver
import android.content.Context
import android.database.MatrixCursor
import android.net.Uri
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Albums
import android.provider.MediaStore.Audio.Media
import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.test.mock.MockContentResolver
import fr.nihilus.mymusic.media.mock.MockCursorProvider
import fr.nihilus.mymusic.utils.MediaID
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@SmallTest
@RunWith(AndroidJUnit4::class)
class MediaDaoTest {

    private lateinit var subject: MediaDao
    private val mockProvider = MockCursorProvider()
    private val mockResolver = MockContentResolver()

    @Before
    fun setUp() {
        val context = mock(Context::class.java)
        `when`<ContentResolver>(context.contentResolver).thenReturn(mockResolver)
        mockResolver.addProvider(MediaStore.AUTHORITY, mockProvider)

        subject = MediaDao(context)
    }

    @Test
    fun allTracks_cursorToMetadata() {
        val cursor = getMockMetadataCursor()
        mockProvider.registerQueryResult(Media.EXTERNAL_CONTENT_URI, cursor)

        val observer = subject.getAllTracks().test().assertValueCount(1)

        val metadataList = observer.values()[0]
        assertThat(metadataList, hasSize(3))
        assertMetadata(metadataList[0], mediaId = "1", title = "Title", album = "Album",
                artist = "Artist", duration = 123L, discNo = 1L, trackNo = 1L,
                artUri = artUriOf(1).toString())
        assertMetadata(metadataList[1], mediaId = "2", title = "Amerika", album = "Reise Reise",
                artist = "Rammstein", duration = 3046L, discNo = 1L, trackNo = 6L,
                artUri = artUriOf(2).toString())

        observer.dispose()
    }

    @Test
    fun allTracks_notifyWhenModified() {
        val cursor = getMockMetadataCursor()
        mockProvider.registerQueryResult(Albums.EXTERNAL_CONTENT_URI, cursor)

        val observer = subject.getAllTracks().test()
        mockResolver.notifyChange(Media.EXTERNAL_CONTENT_URI, null)

        observer.assertValueCount(2)
    }

    @Test
    fun getAlbums_cursorToAlbums() {
        val cursor = getMockAlbumCursor()
        mockProvider.registerQueryResult(Albums.EXTERNAL_CONTENT_URI, cursor)

        val observer = subject.getAlbums().test().assertValueCount(1)

        val albums = observer.values()[0]
        assertThat(albums, hasSize(3))
        assertMediaDescription(albums[0], mediaId = "${MediaID.ID_ALBUMS}/1", title = "Album",
                subtitle = "Artist", iconUri = artUriOf(1), extras = mapOf(
                MediaItems.EXTRA_ALBUM_KEY to "AlbumKey",
                MediaItems.EXTRA_NUMBER_OF_TRACKS to 8,
                MediaItems.EXTRA_YEAR to 2017
        ))

        observer.dispose()
    }
}

// Test materials
private fun artUriOf(musicId: Long) = Uri.parse("content://media/external/audio/albumart/$musicId")

private val METADATA = arrayOf(
        arrayOf(1L, "Title", "Album", "Artist", 123, 101, "TitleKey", "AlbumKey", 1L, 1L, ""),
        arrayOf(2L, "Amerika", "Reise Reise", "Rammstein", 3046, 106, "Amerika", "ReiseReise", 2L, 2L, ""),
        arrayOf(42L, "Fever", "Fever", "Bullet For My Valentine", 2567, 101, "Fever", "Fever", 3L, 3L, "")
)

private val ALBUMS = arrayOf(
        arrayOf(1L, "Album", "AlbumKey", "Artist", 2017, 8),
        arrayOf(18L, "Reise Reise", "ReiseReise", "Rammstein", 2001, 10),
        arrayOf(1664L, "Fever", "Fever", "Bullet For My Valentine", 2010, 11)
)

private fun getMockMetadataCursor(): MatrixCursor {
    val columns = arrayOf(BaseColumns._ID, Media.TITLE, Media.ALBUM, Media.ARTIST, Media.DURATION,
            Media.TRACK, Media.TITLE_KEY, Media.ALBUM_KEY,
            Media.ALBUM_ID, Media.ARTIST_ID, Media.DATA)

    val cursor = MatrixCursor(columns)
    for (row in METADATA) {
        cursor.addRow(row)
    }

    return cursor
}

private fun getMockAlbumCursor(): MatrixCursor {
    val columns = arrayOf(BaseColumns._ID, Albums.ALBUM, Albums.ALBUM_KEY, Albums.ARTIST,
            Albums.LAST_YEAR, Albums.NUMBER_OF_SONGS)
    val cursor = MatrixCursor(columns)
    for (row in ALBUMS) {
        cursor.addRow(row)
    }

    return cursor
}

/**
 * Assert that the given [MediaMetadataCompat] has the expected mediaId, title, album, artist,
 * duration, disc number, track number and album art Uri.
 */
private fun assertMetadata(meta: MediaMetadataCompat, mediaId: String, title: String,
                           album: String, artist: String, duration: Long, discNo: Long,
                           trackNo: Long, artUri: String) {
    assertThat(meta.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID), equalTo(mediaId))
    assertThat(meta.getString(MediaMetadataCompat.METADATA_KEY_TITLE), equalTo(title))
    assertThat(meta.getString(MediaMetadataCompat.METADATA_KEY_ALBUM), equalTo(album))
    assertThat(meta.getString(MediaMetadataCompat.METADATA_KEY_ARTIST), equalTo(artist))
    assertThat(meta.getLong(MediaMetadataCompat.METADATA_KEY_DURATION), equalTo(duration))
    assertThat(meta.getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER), equalTo(discNo))
    assertThat(meta.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER), equalTo(trackNo))
    assertThat(meta.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI), equalTo(artUri))
}

/**
 * Assert that the given [MediaBrowserCompat.MediaItem] has the expected mediaId, title, subtitle,
 * icon Uri and extras.
 */
private fun assertMediaDescription(descr: MediaDescriptionCompat, mediaId: String, title: CharSequence,
                                   subtitle: CharSequence, iconUri: Uri, extras: Map<String, Any>) {
    assertThat(descr.mediaId, equalTo(mediaId))
    assertThat(descr.title, equalTo(title))
    assertThat(descr.subtitle, equalTo(subtitle))
    assertThat(descr.iconUri, equalTo(iconUri))

    for ((key, value) in extras) {
        assertThat(descr.extras?.get(key), equalTo(value))
    }
}
