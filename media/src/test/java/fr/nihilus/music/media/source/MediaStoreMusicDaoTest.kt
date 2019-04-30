/*
 * Copyright 2018 Thibault Seisel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.nihilus.music.media.source

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore.Audio.*
import android.support.v4.media.MediaMetadataCompat
import android.util.LongSparseArray
import fr.nihilus.music.media.*
import fr.nihilus.music.media.utils.PermissionDeniedException
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willThrow
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaStoreMusicDaoTest {

    @[Rule JvmField] val mockitoRule: MockitoRule = MockitoJUnit.rule().silent()

    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockResolver: ContentResolver

    private val metadataCache = LongSparseArray<MediaMetadataCompat>()
    private lateinit var subject: MediaStoreMusicDao

    @Before
    fun setUp() {
        given(mockContext.contentResolver).willReturn(mockResolver)
        subject = MediaStoreMusicDao(mockContext, metadataCache)
    }

    @After
    fun tearDown() {
        // Clear cache and query results mappings, effectively recycling them
        metadataCache.clear()
    }

    @Test
    fun getTracks_whenStoreEmpty_emitsNothing() {
        val cursor = mockTracksCursor()
        given(mockResolver.query(eq(Media.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(cursor)

        subject.getTracks(null, null).test()
            .assertNoErrors()
            .assertNoValues()
            .assertComplete()
    }

    @Test
    fun getTracks_whenDeniedPermission_emitsException() {
        given(mockContext.checkPermission(eq(Manifest.permission.READ_EXTERNAL_STORAGE), anyInt(), anyInt()))
            .willReturn(PackageManager.PERMISSION_DENIED)

        val cursor = mockTracksCursor(0, 1, 2, 3)
        given(mockResolver.query(eq(Media.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(cursor)

        subject.getTracks(null, null).test()
            .assertNoValues()
            .assertError { it is PermissionDeniedException && it.permission == Manifest.permission.READ_EXTERNAL_STORAGE }
    }

    @Test
    fun getTracks_emitItemsFromStore() {
        val cursor = mockTracksCursor(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        given(mockResolver.query(eq(Media.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(cursor)

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
        val albumsCursor = mockAlbumCursor(0, 1, 2, 3, 4, 5, 6, 7)
        given(mockResolver.query(eq(Media.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(cursor)
        given(mockResolver.query(eq(Albums.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(albumsCursor)

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
        given(mockResolver.query(eq(Media.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(cursor)

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
        // Given a query of MediaStore tracks that fails...
        given(mockResolver.query(eq(Media.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(null)

        // Then the stream of metadata should complete without emitting elements.
        subject.getTracks(null, null).test()
            .assertNoErrors()
            .assertNoValues()
            .assertComplete()
    }

    @Test
    fun getTracks_whenNoStoragePermission_emitsNothing() {
        // Simulate a denied permission to read/write external storage
        given(mockContext.checkPermission(
            eq(Manifest.permission.READ_EXTERNAL_STORAGE),
            anyInt(),
            anyInt()
        )).willReturn(PackageManager.PERMISSION_DENIED)

        willThrow(SecurityException())
            .given(mockResolver).query(eq(Media.EXTERNAL_CONTENT_URI), any(), any(), any(), any())

        subject.getTracks(null, null).test()
            .assertNoValues()
            .assertError { it is PermissionDeniedException && it.permission == Manifest.permission.READ_EXTERNAL_STORAGE }
    }

    @Test
    fun getTracks_withSorting_emitsFromStoreInOrder() {
        val cursor = mockTracksCursor(2, 9, 4)
        given(mockResolver.query(eq(Media.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(cursor)

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
        given(mockResolver.query(eq(Media.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(cursor)

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
        given(mockResolver.query(eq(Media.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(cursor)

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
        given(mockResolver.query(eq(Albums.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(cursor)

        subject.getAlbums(null, null).test()
            .assertNoErrors()
            .assertNoValues()
            .assertComplete()
    }

    @Test
    fun getAlbums_emitsAlbumsFromStore() {
        val cursor = mockAlbumCursor(0, 1, 2, 3, 4, 5, 6, 7)
        given(mockResolver.query(eq(Albums.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(cursor)

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
        given(mockResolver.query(eq(Albums.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(cursor)

        val album = subject.getAlbums(null, null).test()
            .assertNoErrors()
            .assertValueCount(1)
            .assertComplete()
            .values()[0]

        with(album) {

            assertEquals("$CATEGORY_ALBUMS/40", mediaId)
            assertEquals("The 2nd Law", title)
            assertEquals("Muse", subtitle)
            val expectedAlbumArtUri = "file:///storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627051019".toUri()
            assertEquals(expectedAlbumArtUri, iconUri)

            extras?.run {
                assertEquals(3, size())
                assertEquals("""C/?)U""", getString(MediaItems.EXTRA_ALBUM_KEY))
                assertEquals(1, getInt(MediaItems.EXTRA_NUMBER_OF_TRACKS))
                assertEquals(2012, getInt(MediaItems.EXTRA_YEAR))
            } ?: Assert.fail("Albums should have extras")
        }
    }

    @Test
    fun getArtists_emptyStore_completesWithNoItems() {
        val artistCursor = mockArtistCursor()
        val albumCursor = mockAlbumCursor(0, 1, 2, 3, 4, 5, 6, 7)
        given(mockResolver.query(eq(Artists.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(artistCursor)
        given(mockResolver.query(eq(Albums.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(albumCursor)

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
        given(mockResolver.query(eq(Artists.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(artistCursor)
        given(mockResolver.query(eq(Albums.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(albumCursor)

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
    fun getArtists_whenNoAlbumInformation_emitsArtistsWithoutArt() {
        val artistCursor = mockArtistCursor(0, 1, 2, 3, 4)
        val albumCursor = mockAlbumCursor()
        given(mockResolver.query(eq(Artists.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(artistCursor)
        given(mockResolver.query(eq(Albums.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(albumCursor)

        val artists = subject.getArtists().test()
            .assertNoErrors()
            .assertValueCount(5)
            .assertComplete()
            .values()

        longArrayOf(5L, 26L, 4L, 13L, 18L)
            .map { mediaIdOf(CATEGORY_ARTISTS, it.toString()) }
            .zip(artists) { expectedId, artist ->
                assertEquals(expectedId, artist.mediaId)
                assertNull(artist.iconUri)
            }
    }

    @Test
    fun getArtists_whenMissingSomeAlbumInformation_emitsWithoutArt() {
        val artistCursor = mockArtistCursor(0, 1, 2, 3, 4)
        val albumCursor = mockAlbumCursor(/*3, 1,*/ 6, 2, 5, 7, 0, 4)
        given(mockResolver.query(eq(Artists.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(artistCursor)
        given(mockResolver.query(eq(Albums.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(albumCursor)

        val artists = subject.getArtists().test()
            .assertNoErrors()
            .assertValueCount(5)
            .assertComplete()
            .values()

        listOf(
            "$CATEGORY_ARTISTS/5" to null,
            "$CATEGORY_ARTISTS/26" to null,
            "$CATEGORY_ARTISTS/4" to "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509626949249",
            "$CATEGORY_ARTISTS/13" to "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627413029",
            "$CATEGORY_ARTISTS/18" to "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627051019"
        ).zip(artists) { (expectedId, expectedAlbumArtPath), actualArtist ->
            assertEquals(expectedId, actualArtist.mediaId)
            assertEquals(expectedAlbumArtPath, actualArtist.iconUri?.path)
        }
    }

    @Test
    fun getArtists_whenMoreAlbumsThanArtists_emitsCorrectArt() {
        val artistCursor = mockArtistCursor(1, 2, 4)
        val albumCursor = mockAlbumCursor(3, 1, 6, 2, 5, 7, 0, 4)
        given(mockResolver.query(eq(Artists.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(artistCursor)
        given(mockResolver.query(eq(Albums.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(albumCursor)

        val artists = subject.getArtists().test()
            .assertNoErrors()
            .assertValueCount(3)
            .assertComplete()
            .values()

        val expectedAlbumArtPaths = listOf(
            "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509626970548",
            "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509626949249",
            "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627051019"
        )

        artists.zip(expectedAlbumArtPaths) { actualArtist, expectedArtPath ->
            assertEquals(expectedArtPath, actualArtist.iconUri?.path)
        }
    }

    @Test
    fun getArtists_translatesRequiredProperties() {
        // Select artist "Avenged Sevenfold" with their album "Nightmare".
        // The selection of iconUri is covered by another test
        val artistCursor = mockArtistCursor(2)
        val albumCursor = mockAlbumCursor(6)
        given(mockResolver.query(eq(Artists.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(artistCursor)
        given(mockResolver.query(eq(Albums.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(albumCursor)

        val artist = subject.getArtists().test()
            .assertNoErrors()
            .assertValueCount(1)
            .assertComplete()
            .values()[0]

        with(artist) {
            assertEquals("$CATEGORY_ARTISTS/4", mediaId)
            assertEquals("Avenged Sevenfold", title)
            val expectedUri = "file:///storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509626949249".toUri()
            assertEquals(expectedUri, iconUri)

            extras?.run {
                assertEquals(2, size())
                assertEquals(
                    """)S1C51/M1S1C3E?/""",
                    getString(MediaItems.EXTRA_TITLE_KEY)
                )
                assertEquals(1, getInt(MediaItems.EXTRA_NUMBER_OF_TRACKS))
            } ?: Assert.fail("Artists should have extras")
        }
    }

    @Test
    fun getArtists_iconIsMostRecentAlbumArt() {
        // Select artist "Foo Fighters" and 3 of their albums
        val artistCursor = mockArtistCursor(3)
        val albumCursor = mockAlbumCursor(2, 5, 7)
        given(mockResolver.query(eq(Artists.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(artistCursor)
        given(mockResolver.query(eq(Albums.EXTERNAL_CONTENT_URI), any(), any(), any(), any())).willReturn(albumCursor)

        val artist = subject.getArtists().test()
            .assertNoErrors()
            .assertValueCount(1)
            .assertComplete()
            .values()[0]

        // The expected icon Uri is the album art of "Concrete and Gold" (ID = 102)
        val expectedIconUri = Uri.parse("file:///storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627413029")
        assertEquals(expectedIconUri, artist.iconUri)
    }
}

fun assertMetadataKeyEquals(
    expected: MediaMetadataCompat,
    actual: MediaMetadataCompat,
    key: String
) = assertEquals(expected.bundle.get(key), actual.bundle.get(key))

