package fr.nihilus.music.media.source

import android.content.Context
import android.provider.MediaStore
import android.provider.MediaStore.Audio.*
import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import android.support.v4.media.MediaMetadataCompat
import android.test.mock.MockContentResolver
import android.util.LongSparseArray
import fr.nihilus.music.assertMetadataKeyEquals
import fr.nihilus.music.media.CATEGORY_ALBUMS
import fr.nihilus.music.media.CATEGORY_ARTISTS
import fr.nihilus.music.media.MediaItems
import fr.nihilus.music.media.mediaIdOf
import fr.nihilus.music.media.mock.MockCursorProvider
import fr.nihilus.music.mock
import fr.nihilus.music.utils.hasExternalStoragePermission
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any

@Suppress("FunctionName")
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
        // Instantiate the class under test
        subject = MediaStoreMusicDao(mockContext, metadataCache)
    }

    @After
    fun tearDown() {
        // Clear cache and query results mappings, effectively recycling them
        metadataCache.clear()
        mockProvider.reset()
    }

    @Test
    fun getTracks_whenStoreEmpty_emitsNothing() {
        val cursor = mockTracksCursor()
        mockProvider.registerQueryResult(Media.EXTERNAL_CONTENT_URI, cursor)

        subject.getTracks(null, null).test()
            .assertNoErrors()
            .assertNoValues()
            .assertComplete()
    }

    @Test
    fun getTracks_emitItemsFromStore() {
        val cursor = mockTracksCursor(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        mockProvider.registerQueryResult(Media.EXTERNAL_CONTENT_URI, cursor)

        val observer = subject.getTracks(null, null).test()
        with(observer) {
            assertNoErrors()
            assertValueCount(cursor.count)
            assertComplete()

            // Assert that tracks are sorted by TITLE
            (0..9).map {
                cursor.moveToPosition(it)
                cursor.getString(1)
            }.forEachIndexed { index, expectedTitle ->
                    assertEquals(
                        expectedTitle,
                        values()[index].getString(MediaMetadataCompat.METADATA_KEY_TITLE)
                    )
                }
        }
    }

    @Test
    fun getTracks_translatesRequiredMetadataKey() {
        val cursor = mockTracksCursor(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        mockProvider.registerQueryResult(Media.EXTERNAL_CONTENT_URI, cursor)

        val observer = subject.getTracks(null, null).test()
            .assertNoErrors()
            .assertValueCount(10)
            .assertComplete()

        // Check that each metadata key is translated correctly from MediaStore's data
        observer.values().forEachIndexed { index, actual ->
            val expected = mockMetadata[index]
            assertMetadataKeyEquals(expected, actual, MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
            assertMetadataKeyEquals(expected, actual, MediaMetadataCompat.METADATA_KEY_TITLE)
            assertMetadataKeyEquals(expected, actual, MediaMetadataCompat.METADATA_KEY_ALBUM)
            assertMetadataKeyEquals(expected, actual, MediaMetadataCompat.METADATA_KEY_ARTIST)
            assertMetadataKeyEquals(expected, actual, MediaMetadataCompat.METADATA_KEY_DURATION)
            assertMetadataKeyEquals(expected, actual, MediaMetadataCompat.METADATA_KEY_DISC_NUMBER)
            assertMetadataKeyEquals(expected, actual, MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER)
            assertMetadataKeyEquals(
                expected,
                actual,
                MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI
            )
            assertMetadataKeyEquals(expected, actual, MediaMetadataCompat.METADATA_KEY_MEDIA_URI)
            assertMetadataKeyEquals(expected, actual, MusicDao.METADATA_KEY_TITLE_KEY)
            assertMetadataKeyEquals(expected, actual, MusicDao.METADATA_KEY_DATE)
            assertMetadataKeyEquals(expected, actual, MusicDao.METADATA_KEY_ARTIST_ID)
            assertMetadataKeyEquals(expected, actual, MusicDao.METADATA_KEY_ALBUM_ID)
        }

    }

    @Test
    fun getTracks_whenCacheEmpty_fillCacheFromStore() {
        val cursor = mockTracksCursor(0, 1, 2, 3)
        mockProvider.registerQueryResult(Media.EXTERNAL_CONTENT_URI, cursor)

        val observer = subject.getTracks(null, null).test()
            .assertNoErrors()
            .assertComplete()
        assertEquals(observer.valueCount(), metadataCache.size())

        (0..3).map {
            cursor.moveToPosition(it)
            cursor.getLong(0)
        }.forEach { musicId -> assertNotNull(metadataCache[musicId]) }
    }

    @Test
    fun getTracks_whenQueryFails_completesWithNoItems() {
        // When a query fails, ContentResolver.query returns a null cursor
        mockProvider.registerQueryResult(Media.EXTERNAL_CONTENT_URI, null)

        subject.getTracks(null, null).test()
            .assertNoErrors()
            .assertNoValues()
            .assertComplete()
    }

    //@Test
    fun getTracks_whenNoStoragePermission_emitsNothing() {
        // Simulate a denied permission to read/write external storage
        // FIXME Test cannot be performed due to Kotlin object being final
        `when`(any<Context>().hasExternalStoragePermission()).thenReturn(false)

        val cursor = mockTracksCursor(0, 1, 2, 3)
        mockProvider.registerQueryResult(Media.EXTERNAL_CONTENT_URI, cursor)

        subject.getTracks(null, null).test()
            .assertNoErrors()
            .assertNoValues()
            .assertComplete()
    }

    @Test
    fun getTracks_withSorting_emitsFromStoreInOrder() {
        val cursor = mockTracksCursor(2, 9, 4)
        mockProvider.registerQueryResult(Media.EXTERNAL_CONTENT_URI, cursor)

        val sorting = "${MusicDao.METADATA_KEY_DATE} DESC"
        val observer = subject.getTracks(null, sorting).test()
            .assertNoErrors()
            .assertValueCount(3)
            .assertComplete()

        arrayOf(2, 9, 4).map(mockMetadata::get)
            .zip(observer.values())
            .forEach { (expected, actual) ->
                assertMetadataKeyEquals(expected, actual, MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
            }
    }

    @Test
    fun getTracks_withStringCriterion_emitsOnlyMatching() {
        val cursor = mockTracksCursor(2, 6, 8, 9)
        mockProvider.registerQueryResult(Media.EXTERNAL_CONTENT_URI, cursor)

        val criteria = mapOf(MediaMetadataCompat.METADATA_KEY_ARTIST to "Foo Fighters")
        val observer = subject.getTracks(criteria, null).test()
            .assertNoErrors()
            .assertValueCount(4)
            .assertComplete()

        intArrayOf(2, 6, 8, 9)
            .map(mockMetadata::get)
            .zip(observer.values())
            .forEach { (expected, actual) ->
                assertMetadataKeyEquals(expected, actual, MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
            }
    }

    @Test
    fun getTracks_withLongCriterion_emitsOnlyMatching() {
        val cursor = mockTracksCursor(2, 6, 8, 9)
        mockProvider.registerQueryResult(Media.EXTERNAL_CONTENT_URI, cursor)

        val criteria = mapOf(MusicDao.METADATA_KEY_ARTIST_ID to 13L)
        val observer = subject.getTracks(criteria, null).test()
            .assertNoErrors()
            .assertValueCount(4)
            .assertComplete()

        intArrayOf(2, 6, 8, 9)
            .map(mockMetadata::get)
            .zip(observer.values())
            .forEach { (expected, actual) ->
                assertMetadataKeyEquals(expected, actual, MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
            }
    }

    @Test
    fun getAlbums_whenStoreEmpty_completesWithNoItems() {
        val cursor = mockAlbumCursor()
        mockProvider.registerQueryResult(Albums.EXTERNAL_CONTENT_URI, cursor)

        subject.getAlbums(null, null).test()
            .assertNoErrors()
            .assertNoValues()
            .assertComplete()
    }

    @Test
    fun getAlbums_emitsAlbumsFromStore() {
        val cursor = mockAlbumCursor(0, 1, 2, 3, 4, 5, 6, 7)
        mockProvider.registerQueryResult(Albums.EXTERNAL_CONTENT_URI, cursor)

        val observer = subject.getAlbums(null, null).test()
            .assertNoErrors()
            .assertValueCount(8)
            .assertComplete()

        longArrayOf(40L, 65L, 102L, 7L, 38L, 26L, 6L, 95L)
            .map { mediaIdOf(CATEGORY_ALBUMS, it.toString()) }
            .zip(observer.values()) { expectedId, album ->
                expectedId to album.mediaId
            }
            .forEach { (expectedId, actualId) ->
                assertEquals(expectedId, actualId)
            }
    }

    @Test
    fun getAlbums_translatesRequiredProperties() {
        val cursor = mockAlbumCursor(0)
        mockProvider.registerQueryResult(Albums.EXTERNAL_CONTENT_URI, cursor)

        val album = subject.getAlbums(null, null).test()
            .assertNoErrors()
            .assertValueCount(1)
            .assertComplete()
            .values()[0]

        with(album) {

            assertEquals("$CATEGORY_ALBUMS/40", mediaId)
            assertEquals("The 2nd Law", title)
            assertEquals("Muse", subtitle)
            assertEquals(artUriOf(40L), iconUri)

            extras?.run {
                assertEquals(3, size())
                assertEquals("""C/?)U""", getString(MediaItems.EXTRA_ALBUM_KEY))
                assertEquals(1, getInt(MediaItems.EXTRA_NUMBER_OF_TRACKS))
                assertEquals(2012, getInt(MediaItems.EXTRA_YEAR))
            } ?: fail("Albums should have extras")
        }
    }

    @Test
    fun getArtists_emptyStore_completesWithNoItems() {
        val artistCursor = mockArtistCursor()
        val albumCursor = mockAlbumCursor()
        mockProvider.registerQueryResult(Artists.EXTERNAL_CONTENT_URI, artistCursor)
        mockProvider.registerQueryResult(Albums.EXTERNAL_CONTENT_URI, albumCursor)

        subject.getArtists().test()
            .assertNoErrors()
            .assertNoValues()
            .assertComplete()
    }

    @Test
    fun getArtists_emitsArtistsFromStore() {
        val artistCursor = mockArtistCursor(0, 1, 2, 3, 4)
        // Albums should be sorted by artist name + descending release year
        val albumCursor = mockAlbumCursor(3, 1, 6, 2, 5, 7, 0, 4)
        mockProvider.registerQueryResult(Artists.EXTERNAL_CONTENT_URI, artistCursor)
        mockProvider.registerQueryResult(Albums.EXTERNAL_CONTENT_URI, albumCursor)

        val artists = subject.getArtists().test()
            .assertNoErrors()
            .assertValueCount(5)
            .assertComplete()
            .values()

        longArrayOf(5L, 26L, 4L, 13L, 18L)
            .map { mediaIdOf(CATEGORY_ARTISTS, it.toString()) }
            .zip(artists) { expectedId, artist ->
                expectedId to artist.mediaId
            }
            .forEach { (expectedId, actualId) ->
                assertEquals(expectedId, actualId)
            }
    }

    @Test
    fun getArtists_translatesRequiredProperties() {
        // Select artist "Avenged Sevenfold" with their album "Nightmare".
        // The selection of iconUri is covered by another test
        val artistCursor = mockArtistCursor(2)
        val albumCursor = mockAlbumCursor(6)
        mockProvider.registerQueryResult(Artists.EXTERNAL_CONTENT_URI, artistCursor)
        mockProvider.registerQueryResult(Albums.EXTERNAL_CONTENT_URI, albumCursor)

        val artist = subject.getArtists().test()
            .assertNoErrors()
            .assertValueCount(1)
            .assertComplete()
            .values()[0]

        with(artist) {
            assertEquals("$CATEGORY_ARTISTS/4", mediaId)
            assertEquals("Avenged Sevenfold", title)
            assertEquals(artUriOf(6L), iconUri)

            extras?.run {
                assertEquals(2, size())
                assertEquals(
                    """)S1C51/M1S1C3E?/""",
                    getString(MediaItems.EXTRA_TITLE_KEY)
                )
                assertEquals(1, getInt(MediaItems.EXTRA_NUMBER_OF_TRACKS))
            } ?: fail("Artists should have extras")
        }
    }

    @Test
    fun getArtists_iconIsMostRecentAlbumArt() {
        // Select artist "Foo Fighters" and 3 of their albums
        val artistCursor = mockArtistCursor(3)
        val albumCursor = mockAlbumCursor(2, 5, 7)
        mockProvider.registerQueryResult(Artists.EXTERNAL_CONTENT_URI, artistCursor)
        mockProvider.registerQueryResult(Albums.EXTERNAL_CONTENT_URI, albumCursor)

        val artist = subject.getArtists().test()
            .assertNoErrors()
            .assertValueCount(1)
            .assertComplete()
            .values()[0]

        // The expected icon Uri is the album art of "Concrete and Gold" (ID = 102)
        val expectedIconUri = artUriOf(102L)
        assertEquals(expectedIconUri, artist.iconUri)
    }
}

