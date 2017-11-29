package fr.nihilus.music.media.source

import android.content.ContentResolver
import android.content.Context
import android.database.MatrixCursor
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.MediaStore.Audio.*
import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import android.test.mock.MockContentResolver
import fr.nihilus.music.assertMediaDescription
import fr.nihilus.music.media.MediaItems
import fr.nihilus.music.media.artUriOf
import fr.nihilus.music.media.assertMetadata
import fr.nihilus.music.media.mock.MockCursorProvider
import fr.nihilus.music.utils.MediaID
import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@SmallTest
@RunWith(AndroidJUnit4::class)
class MediaStoreMusicDaoTest {

    private lateinit var subject: MediaStoreMusicDao
    private val mockProvider = MockCursorProvider()
    private val mockResolver = MockContentResolver()

    @Mock lateinit var context: Context

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        `when`<ContentResolver>(context.contentResolver).thenReturn(mockResolver)
        mockResolver.addProvider(MediaStore.AUTHORITY, mockProvider)

        subject = MediaStoreMusicDao(context)
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
        assertThat(albums, hasSize(cursor.count))
        assertMediaDescription(albums[0], mediaId = "${MediaID.ID_ALBUMS}/1", title = "Album",
                subtitle = "Artist", iconUri = artUriOf(1L), extras = mapOf(
                MediaItems.EXTRA_ALBUM_KEY to "AlbumKey",
                MediaItems.EXTRA_NUMBER_OF_TRACKS to 8,
                MediaItems.EXTRA_YEAR to 2017
        ))

        observer.dispose()
    }

    @Test
    fun getArtists_cursorToArtists() {
        val artistCursor = getMockArtistCursor()
        val albumCursor = getMockAlbumCursor()
        mockProvider.registerQueryResult(Artists.EXTERNAL_CONTENT_URI, artistCursor)
        mockProvider.registerQueryResult(Albums.EXTERNAL_CONTENT_URI, albumCursor)

        val observer = subject.getArtists().test().assertValueCount(1)

        val artists = observer.values()[0]
        assertThat(artists, hasSize(artistCursor.count))
        assertMediaDescription(artists[0], mediaId = "${MediaID.ID_ARTISTS}/1", title = "Artist",
                subtitle = null, iconUri = artUriOf(1L), extras = mapOf(
                MediaItems.EXTRA_TITLE_KEY to "ArtistKey",
                MediaItems.EXTRA_NUMBER_OF_TRACKS to 23
        ))

        observer.dispose()
    }

    @Test
    fun getArtists_mostRecentAlbumArt() {
        val artistCursor = getMockArtistCursor()
        val albumCursor = getMockAlbumCursor()
        mockProvider.registerQueryResult(Artists.EXTERNAL_CONTENT_URI, artistCursor)
        mockProvider.registerQueryResult(Albums.EXTERNAL_CONTENT_URI, albumCursor)

        val observer = subject.getArtists().test()

        val artists = observer.values()[0]
        assertThat(artists, hasSize(artistCursor.count))
        val rammsteinArtist = artists[1]

        assertThat(rammsteinArtist.iconUri, `is`(equalTo(artUriOf(18L))))
    }
}

// Test materials

private val METADATA = arrayOf(
        arrayOf(1L, "Title", "Album", "Artist", 123, 101, "TitleKey", "AlbumKey", 1L, 1L, ""),
        arrayOf(2L, "Amerika", "Reise Reise", "Rammstein", 3046, 106, "Amerika", "ReiseReise", 2L, 2L, ""),
        arrayOf(42L, "Fever", "Fever", "Bullet For My Valentine", 2567, 101, "Fever", "Fever", 3L, 3L, "")
)

private val ALBUMS = arrayOf(
        arrayOf(1L, "Album", "AlbumKey", "Artist", 2017, 8),
        arrayOf(18L, "Reise Reise", "ReiseReise", "Rammstein", 2001, 10),
        arrayOf(24L, "Sehnsucht", "Sehnsucht", "Rammstein", 1999, 9),
        arrayOf(1664L, "Fever", "Fever", "Bullet For My Valentine", 2010, 11)
)

private val ARTISTS = arrayOf(
        arrayOf(1L, "Artist", "ArtistKey", 23),
        arrayOf(2L, "Rammstein", "Rammstein", 36)
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

private fun getMockArtistCursor(): MatrixCursor {
    val columns = arrayOf(Artists._ID, Artists.ARTIST, Artists.ARTIST_KEY, Artists.NUMBER_OF_TRACKS)
    val cursor = MatrixCursor(columns)
    for (row in ARTISTS) {
        cursor.addRow(row)
    }

    return cursor
}

