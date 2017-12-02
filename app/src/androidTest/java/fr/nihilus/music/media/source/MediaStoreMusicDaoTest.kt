package fr.nihilus.music.media.source

import android.content.Context
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import android.support.v4.media.MediaMetadataCompat
import android.test.mock.MockContentResolver
import android.util.LongSparseArray
import fr.nihilus.music.assertMetadataKeyEquals
import fr.nihilus.music.media.mock.MockCursorProvider
import fr.nihilus.music.mock
import fr.nihilus.music.utils.PermissionUtil
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any

@SmallTest
@RunWith(AndroidJUnit4::class)
class MediaStoreMusicDaoTest {

    private val metadataCache = LongSparseArray<MediaMetadataCompat>()
    private val mockProvider = MockCursorProvider()

    private val mockResolver = MockContentResolver().apply {
        addProvider(MediaStore.AUTHORITY, mockProvider)
    }

    private val mockContext = mock<Context>().apply {
        `when`(contentResolver).thenReturn(mockResolver)
    }

    private lateinit var subject: MediaStoreMusicDao

    @Before
    fun setUp() {
        subject = MediaStoreMusicDao(mockContext, metadataCache)
    }

    @After
    fun tearDown() {
        metadataCache.clear()
        mockProvider.reset()
    }

    @Test
    fun getTracks_whenStoreEmpty_emitsNothing() {
        val cursor = mockTracksCursor()
        mockProvider.registerQueryResult(Media.EXTERNAL_CONTENT_URI, cursor)

        val observer = subject.getTracks(null, null).test()
        with(observer) {
            assertNoValues()
            assertComplete()
        }
    }

    @Test
    fun getTracks_emitItemsfromStore() {
        val cursor = mockTracksCursor(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        mockProvider.registerQueryResult(Media.EXTERNAL_CONTENT_URI, cursor)

        val observer = subject.getTracks(null, null).test()
        with(observer) {
            assertValueCount(cursor.count)
            assertComplete()

            // Assert that tracks are sorted by TITLE
            (0..9).map {
                cursor.moveToPosition(it)
                cursor.getString(1)
            }.forEachIndexed { index, expectedTitle ->
                assertEquals(expectedTitle,
                        values()[index].getString(MediaMetadataCompat.METADATA_KEY_TITLE))
            }
        }
    }

    @Test
    fun getTracks_translatesRequiredMetadataKey() {
        val cursor = mockTracksCursor(0, 1, 2)
        mockProvider.registerQueryResult(Media.EXTERNAL_CONTENT_URI, cursor)

        val observer = subject.getTracks(null, null).test()
        with(observer) {
            assertValueCount(3)

            observer.values().forEachIndexed { index, actual ->
                val expected = mockMetadata[index]
                assertMetadataKeyEquals(expected, actual, MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                assertMetadataKeyEquals(expected, actual, MediaMetadataCompat.METADATA_KEY_TITLE)
                assertMetadataKeyEquals(expected, actual, MediaMetadataCompat.METADATA_KEY_ALBUM)
                assertMetadataKeyEquals(expected, actual, MediaMetadataCompat.METADATA_KEY_ARTIST)
                assertMetadataKeyEquals(expected, actual, MediaMetadataCompat.METADATA_KEY_DURATION)
                assertMetadataKeyEquals(expected, actual, MediaMetadataCompat.METADATA_KEY_DISC_NUMBER)
                assertMetadataKeyEquals(expected, actual, MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER)
                assertMetadataKeyEquals(expected, actual, MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
                assertMetadataKeyEquals(expected, actual, MediaMetadataCompat.METADATA_KEY_MEDIA_URI)
                assertMetadataKeyEquals(expected, actual, MusicDao.METADATA_KEY_TITLE_KEY)
                assertMetadataKeyEquals(expected, actual, MusicDao.METADATA_KEY_DATE_ADDED)
                assertMetadataKeyEquals(expected, actual, MusicDao.METADATA_KEY_ARTIST_ID)
                assertMetadataKeyEquals(expected, actual, MusicDao.METADATA_KEY_ALBUM_ID)
            }

        }
    }

    @Test
    fun getTracks_whenCacheEmpty_fillCacheFromStore() {
        val cursor = mockTracksCursor(0, 1, 2, 3)
        mockProvider.registerQueryResult(Media.EXTERNAL_CONTENT_URI, cursor)

        subject.getTracks(null, null).test()
        assertEquals(cursor.count, metadataCache.size())

        (0..3).map {
            cursor.moveToPosition(it)
            cursor.getLong(0)
        }.forEach { musicId ->
            assertTrue(metadataCache.get(musicId) != null)
        }
    }

    //@Test
    fun getTracks_whenNoStoragePermission_emitsNothing() {
        // Simulate a denied permission to read/write external storage
        // FIXME Test cannot be performed due to Kotlin object being final
        val permissionChecker = mock<PermissionUtil>()
        `when`(permissionChecker.hasExternalStoragePermission(any<Context>())).thenReturn(false)

        val cursor = mockTracksCursor(0, 1, 2, 3)
        mockProvider.registerQueryResult(Media.EXTERNAL_CONTENT_URI, cursor)

        val observer = subject.getTracks(null, null).test()
        with(observer) {
            assertNoValues()
            assertComplete()
        }
    }

    @Test
    fun getTracks_withSorting_emitsFromStoreInOrder() {
        val cursor = mockTracksCursor(2, 9, 4)
        mockProvider.registerQueryResult(Media.EXTERNAL_CONTENT_URI, cursor)

        val sorting = "${MusicDao.METADATA_KEY_DATE_ADDED} DESC"
        val observer = subject.getTracks(null, sorting).test()
        with(observer) {
            assertValueCount(3)
            assertComplete()
        }

        arrayOf(2, 9, 4)
                .map { mockMetadata[it].getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) }
                .zip(observer.values()) { expectedId, actual ->
                    val actualId = actual.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                    assertEquals(expectedId, actualId)
                }
    }

    /*@Test
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
    }*/
}

