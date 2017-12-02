package fr.nihilus.music.media.source

import android.content.Context
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import android.support.v4.media.MediaMetadataCompat
import android.test.mock.MockContentResolver
import android.util.LongSparseArray
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
    fun getTracks_whenCacheEmpty_emitItemsfromStore() {
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

    @Test
    fun getTracks_whenCacheFull_retrieveFromCache() {
        // Fill cache with metadata from the representative test set
        mockMetadata.forEach {
            val musicId = it.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID).toLong()
            metadataCache.put(musicId, it)
        }

        val observer = subject.getTracks(null, null).test()
        with(observer) {
            assertValueCount(mockMetadata.size)
            assertComplete()
        }

        // Check that each track appear in order with the correct id
        val expectedIds = mockMetadata.map { it.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) }
        val actualIds = observer.values().map { it.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) }

        expectedIds.zip(actualIds).forEach { (expected, actual) ->
            assertEquals(expected, actual)
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

        val sorting = "${MusicDao.METADATA_DATE_ADDED} DESC"
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

    @Test
    fun getTracks_withSorting_emitsFromCacheInOrder() {
        with(metadataCache) {
            put(481L, mockMetadata[2])
            put(477L, mockMetadata[9])
            put(125L, mockMetadata[4])
        }

        val sorting = "${MusicDao.METADATA_DATE_ADDED} DESC"
        val observer = subject.getTracks(null, sorting).test()
        with(observer) {
            assertValueCount(3)
            assertComplete()
        }

        arrayOf(2, 9, 4)
                .map { mockMetadata[it].getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) }
                .zip(observer.values()) {expectedId, actual ->
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

