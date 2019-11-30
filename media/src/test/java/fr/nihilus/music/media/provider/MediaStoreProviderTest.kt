/*
 * Copyright 2019 Thibault Seisel
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

package fr.nihilus.music.media.provider

import android.Manifest
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore.Audio.*
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.TruthJUnit.assume
import fr.nihilus.music.core.context.AppDispatchers
import fr.nihilus.music.core.os.FileSystem
import fr.nihilus.music.core.os.PermissionDeniedException
import fr.nihilus.music.core.os.RuntimePermissions
import fr.nihilus.music.core.test.coroutines.CoroutineTestRule
import fr.nihilus.music.core.test.fail
import fr.nihilus.music.core.test.failAssumption
import fr.nihilus.music.core.test.os.DeniedPermission
import fr.nihilus.music.core.test.os.GrantedPermission
import fr.nihilus.music.media.os.BasicFileSystem
import fr.nihilus.music.media.os.MediaStoreDatabase
import fr.nihilus.music.media.os.SimulatedFileSystem
import fr.nihilus.music.media.provider.FailingMediaStore.query
import io.kotlintest.shouldThrow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val MUSIC_FOLDER_NAME = "Music"
private const val TEST_FILENAME = "1741_(The_Battle_of_Cartagena).mp3"

@RunWith(AndroidJUnit4::class)
internal class MediaStoreProviderTest {

    @get:Rule
    val test = CoroutineTestRule()

    private val dispatchers = AppDispatchers(test.dispatcher)
    private lateinit var storeSurrogate: SQLiteMediaStore

    @Before
    fun setUp() {
        storeSurrogate = SQLiteMediaStore(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        storeSurrogate.release()
    }

    @Test
    fun `Given denied permission, when querying tracks then fail with PermissionDeniedException`() {
        assertProviderThrowsWhenPermissionIsDenied(MediaProvider::queryTracks)
    }

    @Test
    fun `Given denied permission, when querying albums then fail with PermissionDeniedException`() {
        assertProviderThrowsWhenPermissionIsDenied(MediaProvider::queryAlbums)
    }

    @Test
    fun `Given denied permission, when querying artists then fail with PermissionDeniedException`() {
        assertProviderThrowsWhenPermissionIsDenied(MediaProvider::queryArtists)
    }

    @Test
    fun `When querying tracks, then there should be all music tracks`() = test.run {
        val provider = MediaStoreProvider()
        val allTracks = provider.queryTracks()
        assertThat(allTracks).hasSize(10)
    }

    @Test
    fun `When querying tracks, then translate metadata to a Track`() = test.run {
        val provider = MediaStoreProvider()
        val allTracks = provider.queryTracks()
        val aTrack = allTracks.find { it.id == 161L } ?: failAssumption("Missing a track with id = 161")

        assertThat(aTrack.id).isEqualTo(161L)
        assertThat(aTrack.title).isEqualTo("1741 (The Battle of Cartagena)")
        assertThat(aTrack.albumId).isEqualTo(65L)
        assertThat(aTrack.album).isEqualTo("Sunset on the Golden Age")
        assertThat(aTrack.artistId).isEqualTo(26L)
        assertThat(aTrack.artist).isEqualTo("Alestorm")
        assertThat(aTrack.duration).isEqualTo(437603L)
        assertThat(aTrack.availabilityDate).isEqualTo(1466283480L)
        assertThat(aTrack.fileSize).isEqualTo(17_506_481L)
    }

    @Test
    fun `When querying tracks, then media uri should be content uri for track`() = test.run {
        val provider = MediaStoreProvider()

        val allTracks = provider.queryTracks()
        val aTrack = allTracks.find { it.id == 161L } ?: failAssumption("Missing a track with id = 161")

        assertThat(aTrack.mediaUri).isEqualTo("${Media.EXTERNAL_CONTENT_URI}/161")
    }

    @Test
    fun `When querying tracks, then disc and track numbers should be calculated`() = test.run {
        val provider = MediaStoreProvider()
        val allTracks = provider.queryTracks()

        // Test with a track present on disc 1 : "Give It Up"
        val trackOnDiscOne = allTracks.find { it.id == 48L } ?: failAssumption("Missing a track on disc 1")
        assertThat(trackOnDiscOne.discNumber).isEqualTo(1)
        assertThat(trackOnDiscOne.trackNumber).isEqualTo(19)

        // Test with a track present on disc 2 : "Jailbreak"
        val trackOnDiscTwo = allTracks.find { it.id == 125L } ?: failAssumption("Missing a track on disc 2")
        assertThat(trackOnDiscTwo.discNumber).isEqualTo(2)
        assertThat(trackOnDiscTwo.trackNumber).isEqualTo(14)
    }

    @Test
    fun `When querying tracks, then resolve content uri from album art path of corresponding album`() = test.run {
        val provider = MediaStoreProvider()
        val allTracks = provider.queryTracks()

        assume().that(allTracks).isNotEmpty()

        val firstTrack = allTracks.first()
        assertThat(firstTrack.albumArtUri).isEqualTo("content://fr.nihilus.music.media.test.provider/albumthumbs/1509626970548")
    }

    @Test
    fun `When querying tracks, then those should be sorted alphabetically without common prefixes`() = test.run {
        val provider = MediaStoreProvider()
        val allTracks = provider.queryTracks()

        assume().that(allTracks.size).isAtLeast(3)
        val trackIds = allTracks.map { it.id }
        assertThat(trackIds).containsExactly(161L, 309L, 481L, 48L, 125L, 294L, 219L, 75L, 464L, 477L).inOrder()
    }

    @Test
    fun `When querying albums, then there should be all albums`() = test.run {
        val provider = MediaStoreProvider()
        val allAlbums = provider.queryAlbums()

        assertThat(allAlbums).hasSize(8)
    }

    @Test
    fun `When querying albums, then translate metadata to an Album`() = test.run {
        val provider = MediaStoreProvider()

        val allAlbums = provider.queryAlbums()
        val anAlbum = allAlbums.find { it.id == 40L } ?: fail("Missing an album with id = 40")

        assertThat(anAlbum.id).isEqualTo(40L)
        assertThat(anAlbum.title).isEqualTo("The 2nd Law")
        assertThat(anAlbum.artistId).isEqualTo(18L)
        assertThat(anAlbum.artist).isEqualTo("Muse")
        assertThat(anAlbum.releaseYear).isEqualTo(2012)
        assertThat(anAlbum.trackCount).isEqualTo(1)
        assertThat(anAlbum.albumArtUri).isEqualTo("content://fr.nihilus.music.media.test.provider/albumthumbs/1509627051019")
    }

    @Test
    fun `When querying albums, then albums should be sorted alphabetically without common prefixes`() = test.run {
        val provider = MediaStoreProvider()

        val allAlbums = provider.queryAlbums()
        assume().that(allAlbums.size).isAtLeast(3)

        val albumIds = allAlbums.map { it.id }
        assertThat(albumIds).containsExactly(40L, 38L, 102L, 95L, 7L, 6L, 65L, 26L).inOrder()
    }

    @Test
    fun `When querying artists, then return all artists`() = test.run {
        val provider = MediaStoreProvider()

        val allArtists = provider.queryArtists()
        assertThat(allArtists).hasSize(5)
    }

    @Test
    fun `When querying artists, then translate metadata to an Artist`() = test.run {
        val provider = MediaStoreProvider()
        val allArtists = provider.queryArtists()
        val anArtist = allArtists.find { it.id == 5L } ?: failAssumption("Missing an artist with id = 5")

        assertThat(anArtist.id).isEqualTo(5L)
        assertThat(anArtist.name).isEqualTo("AC/DC")
        assertThat(anArtist.albumCount).isEqualTo(1)
        assertThat(anArtist.trackCount).isEqualTo(2)
    }

    @Test
    fun `When querying artists, then its icon should be that of its most recent album`() = test.run {
        val provider = MediaStoreProvider()
        val allArtists = provider.queryArtists()

        // Alestorm only have one album here ; its icon should be that of that album
        val artistWithOneAlbum = allArtists.find { it.id == 26L } ?: failAssumption("Missing an artist with id = 26")
        assertThat(artistWithOneAlbum.iconUri).isEqualTo("content://fr.nihilus.music.media.test.provider/albumthumbs/1509626970548")

        // Foo Fighters have 3 albums, use the icon of "Concrete and Gold"
        val artistWithMultipleAlbums = allArtists.find { it.id == 13L } ?: failAssumption("Missing an artist with id = 13")
        assertThat(artistWithMultipleAlbums.iconUri).isEqualTo("content://fr.nihilus.music.media.test.provider/albumthumbs/1509627413029")
    }

    @Test
    fun `When querying artists, then artists should be sorted alphabetically without common prefixes`() = test.run {
        val provider = MediaStoreProvider()
        val allArtists = provider.queryArtists()
        assume().that(allArtists.size).isAtLeast(3)

        val artistIds = allArtists.map { it.id }
        assertThat(artistIds).containsExactly(5L, 26L, 4L, 13L, 18L).inOrder()
    }

    @Test
    fun `When registering track observer, then register a ContentObserver for Media Uri`() {
        assertRegisterObserver(MediaProvider.MediaType.TRACKS, Media.EXTERNAL_CONTENT_URI)
    }

    @Test
    fun `When registering album observer, then register a ContentObserver for Album Uri`() {
        assertRegisterObserver(MediaProvider.MediaType.ALBUMS, Albums.EXTERNAL_CONTENT_URI)
    }

    @Test
    fun `When registering artist observer, then register a ContentObserver for Artist Uri`() {
        assertRegisterObserver(MediaProvider.MediaType.ARTISTS, Artists.EXTERNAL_CONTENT_URI)
    }

    @Test
    fun `Given registered track observer, when unregistering then unregister for Media Uri`() {
        assertUnregisterObserver(MediaProvider.MediaType.TRACKS, Media.EXTERNAL_CONTENT_URI)
    }

    @Test
    fun `Given registered album observer, when unregistering then unregister for Album Uri`() {
        assertUnregisterObserver(MediaProvider.MediaType.ALBUMS, Albums.EXTERNAL_CONTENT_URI)
    }

    @Test
    fun `Given registered artist observer, wen unregistering then unregister for Artist Uri`() {
        assertUnregisterObserver(MediaProvider.MediaType.ARTISTS, Artists.EXTERNAL_CONTENT_URI)
    }

    @Test
    fun `Given failing MediaStore, when querying tracks then return an empty track list`() = test.run {
        assertQueryFailsGracefully(MediaProvider::queryTracks)
    }

    @Test
    fun `Given failing MediaStore, when querying albums then return an empty album list`() = test.run {
        assertQueryFailsGracefully(MediaProvider::queryAlbums)
    }

    @Test
    fun `Given failing MediaStore, when querying artists then return an empty artist list`() = test.run {
        assertQueryFailsGracefully(MediaProvider::queryArtists)
    }

    @Test
    fun `Given denied permission, when deleting tracks then fail with PermissionDeniedException`() = test.run {
        val provider = MediaStoreProvider(permissions = DeniedPermission)
        val exception = shouldThrow<PermissionDeniedException> {
            provider.deleteTracks(longArrayOf(161, 309))
        }

        assertThat(exception.permission).isEqualTo(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    @Test
    fun `When deleting a track, then also delete the corresponding file`() = test.run {
        val simulatedFileSystem = SimulatedFileSystem("$MUSIC_FOLDER_NAME/$TEST_FILENAME")
        val provider = MediaStoreProvider(
            fs = simulatedFileSystem
        )

        provider.deleteTracks(longArrayOf(161L))

        assertThat(simulatedFileSystem.fileExists("$MUSIC_FOLDER_NAME/$TEST_FILENAME")).isFalse()
    }

    @Test
    fun `When a track file is deleted, then delete its metadata from MediaStore`() = test.run {
        val provider = MediaStoreProvider(
            fs = SimulatedFileSystem("$MUSIC_FOLDER_NAME/$TEST_FILENAME")
        )

        provider.deleteTracks(longArrayOf(161L))

        val existsInDatabase = storeSurrogate.exists(MediaProvider.MediaType.TRACKS, 161L)
        assertThat(existsInDatabase).isFalse()
    }

    @Test
    fun `When a track file cannot be deleted, then do not delete its metadata from MediaStore`() = test.run {
        // Files doesn't exists, so deleting them will fail.
        val provider = MediaStoreProvider(fs = SimulatedFileSystem())

        provider.deleteTracks(longArrayOf(161L))

        val existsInDatabase = storeSurrogate.exists(MediaProvider.MediaType.TRACKS, 161L)
        assertThat(existsInDatabase).isTrue()
    }

    private suspend fun assertQueryFailsGracefully(queryFun: suspend MediaProvider.() -> List<Any>) {
        val provider = MediaStoreProvider(store = FailingMediaStore)
        assertThat(provider.queryFun()).isEmpty()
    }

    private fun assertRegisterObserver(observerType: MediaProvider.MediaType, expectedRegisteredUri: Uri) {
        val provider = MediaStoreProvider()
        val observer = NoopObserver(observerType)
        provider.registerObserver(observer)

        val observedUris = storeSurrogate.observers.map { it.uri }
        assertThat(observedUris).contains(expectedRegisteredUri)
    }

    private fun assertUnregisterObserver(observerType: MediaProvider.MediaType, registeredUri: Uri) {
        val provider = MediaStoreProvider()
        val observer = NoopObserver(observerType)
        provider.registerObserver(observer)
        provider.unregisterObserver(observer)

        val observedUris = storeSurrogate.observers.map { it.uri }
        assertThat(observedUris).doesNotContain(registeredUri)
    }

    private fun assertProviderThrowsWhenPermissionIsDenied(
        queryFun: suspend MediaProvider.() -> List<Any>
    ) = test.run {
        val provider = MediaStoreProvider(permissions = DeniedPermission)

        val exception = shouldThrow<PermissionDeniedException> {
            provider.queryFun()
        }

        assertThat(exception.permission).isEqualTo(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun MediaStoreProvider(
        store: MediaStoreDatabase = storeSurrogate,
        fs: FileSystem = BasicFileSystem,
        permissions: RuntimePermissions = GrantedPermission
    ) = MediaStoreProvider(store, fs, permissions, dispatchers)
}

/**
 * A [MediaProvider.Observer] test fixture that does nothing when media structure changes.
 */
private class NoopObserver(
    mediaType: MediaProvider.MediaType
) : MediaProvider.Observer(mediaType) {
    override fun onChanged() = Unit
}

/**
 * A test fixture for a media provider whose query always fail
 * (i.e. [query] always return a `null` cursor).
 */
private object FailingMediaStore : MediaStoreDatabase {

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun delete(uri: Uri, where: String?, whereArgs: Array<String>?): Int = 0

    override fun registerContentObserver(
        uri: Uri,
        notifyForDescendants: Boolean,
        observer: ContentObserver
    ) = Unit

    override fun unregisterContentObserver(observer: ContentObserver) = Unit
}

