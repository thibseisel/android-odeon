/*
 * Copyright 2020 Thibault Seisel
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
import android.net.Uri
import android.provider.MediaStore.Audio.*
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.nihilus.music.core.context.AppDispatchers
import fr.nihilus.music.core.os.FileSystem
import fr.nihilus.music.core.os.PermissionDeniedException
import fr.nihilus.music.core.os.RuntimePermissions
import fr.nihilus.music.core.test.coroutines.CoroutineTestRule
import fr.nihilus.music.core.test.coroutines.test
import fr.nihilus.music.core.test.failAssumption
import fr.nihilus.music.core.test.os.DeniedPermission
import fr.nihilus.music.core.test.os.GrantedPermission
import fr.nihilus.music.media.os.BasicFileSystem
import fr.nihilus.music.media.os.MediaStoreDatabase
import fr.nihilus.music.media.os.SimulatedFileSystem
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.matchers.withClue
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.runner.RunWith
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class MediaStoreDaoTest {

    @get:Rule
    val test = CoroutineTestRule()

    private val dispatchers = AppDispatchers(test.dispatcher)
    private lateinit var fakeMediaStore: SQLiteMediaStore

    @BeforeTest
    fun initDatabase() {
        fakeMediaStore = SQLiteMediaStore(ApplicationProvider.getApplicationContext())
    }

    @AfterTest
    fun releaseDatabase() {
        fakeMediaStore.release()
    }

    @Test
    fun `Given denied read permission, when collecting any flow then fail with PermissionDeniedException`() = test.run {
        val dao = MediaDao(permissions = DeniedPermission)

        dao.tracks.shouldFailDueToMissingExternalStorageReadPermission()
        dao.albums.shouldFailDueToMissingExternalStorageReadPermission()
        dao.artists.shouldFailDueToMissingExternalStorageReadPermission()
    }

    private suspend fun Flow<List<*>>.shouldFailDueToMissingExternalStorageReadPermission() = test {
        val exception = expectFailure()
        exception.shouldBeInstanceOf<PermissionDeniedException> {
            it.permission shouldBe Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    @Test
    fun `Given failing MediaStore, when collecting any flow then emit an empty list`() = test.run {
        val failingDao = MediaDao(store = FailingMediaStore)

        failingDao.tracks.first().shouldBeEmpty()
        failingDao.albums.first().shouldBeEmpty()
        failingDao.artists.first().shouldBeEmpty()
    }

    @Test
    fun `When querying tracks, then list them all in alphabetic order`() = test.run {
        val dao = MediaDao()
        val tracks = dao.tracks.first()

        val trackIds = tracks.map { it.id }
        trackIds.shouldContainExactly(161L, 309L, 865L, 481L, 48L, 125L, 294L, 219L, 75L, 464L, 477L)
    }

    @Test
    fun `When collecting tracks, then map cursor columns to a list of Track`() = test.run {
        val dao = MediaDao()
        val tracks = dao.tracks.first()

        tracks.find { it.id == 161L }?.should { cartagena ->
            cartagena.id shouldBe 161L
            cartagena.title shouldBe "1741 (The Battle of Cartagena)"
            cartagena.albumId shouldBe 65L
            cartagena.album shouldBe "Sunset on the Golden Age"
            cartagena.artistId shouldBe 26L
            cartagena.artist shouldBe "Alestorm"
            cartagena.discNumber shouldBe 1
            cartagena.trackNumber shouldBe 4
            cartagena.duration shouldBe 437603L
            cartagena.availabilityDate shouldBe 1466283480L
            cartagena.fileSize shouldBe 17_506_481L

            withClue("The Uri of a track should be based on the Media external uri and its id") {
                cartagena.mediaUri shouldBe "${Media.EXTERNAL_CONTENT_URI}/161"
            }

        } ?: failAssumption("Expected the track \"1741 (The Battle of Cartagena)\".")

        tracks.find { it.id == 125L }?.should { jailbreak ->
            jailbreak.id shouldBe 125L
            jailbreak.title shouldBe "Jailbreak"
            jailbreak.albumId shouldBe 7L
            jailbreak.album shouldBe "Greatest Hits 30 Anniversary Edition"
            jailbreak.artistId shouldBe 5L
            jailbreak.artist shouldBe "AC/DC"
            jailbreak.discNumber shouldBe 2
            jailbreak.trackNumber shouldBe 14
            jailbreak.duration shouldBe 276668L
            jailbreak.availabilityDate shouldBe 1455310140L
            jailbreak.fileSize shouldBe 6_750_404

            withClue("The Uri of a track should be based on the Media external uri and its id") {
                jailbreak.mediaUri shouldBe "${Media.EXTERNAL_CONTENT_URI}/125"
            }

        } ?: failAssumption("Expected the track \"Jailbreak\".")
    }

    @Test
    fun `When querying tracks, then resolve content uri from album art path of corresponding album`() = test.run {
        val dao = MediaDao()
        val tracks = dao.tracks.first().takeIf { it.isNotEmpty() }
            ?: failAssumption("Expected to have tracks but was empty.")

        val firstTrack = tracks.first()
        firstTrack.albumArtUri shouldBe "content://fr.nihilus.music.media.test.provider/albumthumbs/1509626970548"
    }

    @Test
    fun `When querying albums, then list them all in alphabetic order`() = test.run {
        val dao = MediaDao()

        val albums = dao.albums.first()
        val albumIds = albums.map { it.id }

        albumIds.shouldContainExactly(40L, 38L, 102L, 95L, 7L, 6L, 98L, 65L, 26L)
    }

    @Test
    fun `When querying albums, then map cursor columns to a list of Album`() = test.run {
        val dao = MediaDao()
        val albums = dao.albums.first()
        
        albums.find { it.id == 40L }?.should { secondLaw ->
            secondLaw.id shouldBe 40L
            secondLaw.title shouldBe "The 2nd Law"
            secondLaw.artistId shouldBe 18L
            secondLaw.artist shouldBe "Muse"
            secondLaw.releaseYear shouldBe 2012
            secondLaw.trackCount shouldBe 1
            secondLaw.albumArtUri shouldBe "content://fr.nihilus.music.media.test.provider/albumthumbs/1509627051019"
        } ?: failAssumption("Expected the album \"The 2nd Law\".")
    }
    
    @Test
    fun `When querying artists, then list them all in alphabetic order`() = test.run { 
        val dao = MediaDao()

        val artists = dao.artists.first()
        val artistIds = artists.map { it.id }

        artistIds.shouldContainExactly(5L, 26L, 4L, 13L, 18L)
    }

    @Test
    fun `When querying artists, then map cursor columns to a list of Artist`() = test.run {
        val dao = MediaDao()
        val artists = dao.artists.first()

        artists.find { it.id == 5L }?.should { acdc ->
            acdc.id shouldBe 5L
            acdc.name shouldBe "AC/DC"
            acdc.albumCount shouldBe 1
            acdc.trackCount shouldBe 2
        } ?: failAssumption("Missing the artist \"AC/DC\".")
    }

    @Test
    fun `When querying artists, then its icon should be that of its most recent album`() = test.run {
        val dao = MediaDao()
        val artists = dao.artists.first()

        // Alestorm only have one album here ; its icon should be that of that album
        val alestorm = artists.find { it.id == 26L } ?: failAssumption("Alestorm is missing (id = 26)")
        alestorm.iconUri shouldBe "content://fr.nihilus.music.media.test.provider/albumthumbs/1509626970548"

        // Foo Fighters have 3 albums, use the icon of "Concrete and Gold"
        val fooFighters = artists.find { it.id == 13L } ?: failAssumption("Foo Fighters is missing (id = 13)")
        fooFighters.iconUri shouldBe "content://fr.nihilus.music.media.test.provider/albumthumbs/1509627413029"

        // Muse have 3 albums but the latest one does not have an artwork.
        // Use that of "The 2nd Law" instead.
        withClue("If the latest album does not have an artwork, then the artist icon should be that of the latest album that have one.") {
            val muse = artists.find { it.id == 18L } ?: failAssumption("Muse is missing (id = 18)")
            muse.iconUri shouldBe "content://fr.nihilus.music.media.test.provider/albumthumbs/1509627051019"
        }
    }

    @Test
    fun `While collecting from any flow, then register ContentObserver for each`() = test.run {
        val dao = MediaDao()

        dao.tracks.shouldRegisterAnObserverFor(Media.EXTERNAL_CONTENT_URI)
        dao.albums.shouldRegisterAnObserverFor(Albums.EXTERNAL_CONTENT_URI)
        dao.artists.shouldRegisterAnObserverFor(Artists.EXTERNAL_CONTENT_URI)
    }

    private suspend fun Flow<List<*>>.shouldRegisterAnObserverFor(observedUri: Uri) {
        drop(1).test {

            withClue("An observer should have been registered.") {
                val observers = fakeMediaStore.observers
                observers shouldHaveSize 1

                observers.first().should {
                    it.uri shouldBe observedUri
                    it.notifyForDescendants shouldBe true
                }
            }
        }

        withClue("The observer should have been disposed.") {
            fakeMediaStore.observers.shouldBeEmpty()
        }
    }

    @Test
    fun `Given any flow, when content changed then reload media list`() = test.run {
        val dao = MediaDao()

        dao.tracks.shouldEmitWheneverContentChanged()
        dao.albums.shouldEmitWheneverContentChanged()
        dao.artists.shouldEmitWheneverContentChanged()
    }

    private suspend fun Flow<List<*>>.shouldEmitWheneverContentChanged() = drop(1).test {
        val registeredObservers = fakeMediaStore.observers.takeUnless { it.isEmpty() }
            ?: failAssumption("Assumed at least one ContentObserver to be registered.")
        registeredObservers.forEach { it.observer.onChange(false, null) }

        expect(1)
        expectNone()
    }

    // TODO Test doesn't pass due to being unable to cancel a running query.
    fun `Given any flow, when content changed multiple time quickly then only emit once`() = test.run {
        val dao = MediaDao()

        dao.tracks.shouldConflateConsecutiveChanges()
        dao.albums.shouldConflateConsecutiveChanges()
        dao.artists.shouldConflateConsecutiveChanges()
    }

    private suspend fun Flow<List<*>>.shouldConflateConsecutiveChanges() = drop(1).test {
        val registeredObservers = fakeMediaStore.observers.takeUnless { it.isEmpty() }
            ?: failAssumption("Assumed at least one ContentObserver to be registered.")

        repeat(2) {
            registeredObservers.forEach { it.observer.onChange(false, null) }
        }

        expect(1)
        expectNone()
    }

    @Test
    fun `Given denied permission, when deleting tracks then fail with PermissionDeniedException`() = test.run {
        val dao = MediaDao(permissions = DeniedPermission)
        val exception = shouldThrow<PermissionDeniedException> {
            dao.deleteTracks(longArrayOf(161, 309))
        }

        exception.permission shouldBe Manifest.permission.WRITE_EXTERNAL_STORAGE
    }

    @Test
    fun `When deleting a track, then also delete the corresponding file`() = test.run {
        val simulatedFileSystem = SimulatedFileSystem("$MUSIC_FOLDER_NAME/$TEST_FILENAME")
        val dao = MediaDao(fs = simulatedFileSystem)

        dao.deleteTracks(longArrayOf(161L))

        simulatedFileSystem.fileExists("$MUSIC_FOLDER_NAME/$TEST_FILENAME") shouldBe false
    }

    @Test
    fun `When a track file is deleted, then delete its metadata from MediaStore`() = test.run {
        val dao = MediaDao(
            fs = SimulatedFileSystem("$MUSIC_FOLDER_NAME/$TEST_FILENAME")
        )

        dao.deleteTracks(longArrayOf(161L))

        fakeMediaStore.mediaExists(161L) shouldBe false
    }

    @Test
    fun `When a track file cannot be deleted, then do not delete its metadata from MediaStore`() = test.run {
        // Files doesn't exists, so deleting them will fail.
        val dao = MediaDao(fs = SimulatedFileSystem())

        dao.deleteTracks(longArrayOf(161L))

        fakeMediaStore.mediaExists(161L) shouldBe true
    }

    private fun MediaDao(
        store: MediaStoreDatabase = fakeMediaStore,
        fs: FileSystem = BasicFileSystem,
        permissions: RuntimePermissions = GrantedPermission
    ) = MediaStoreDao(store, fs, permissions, dispatchers)
}

private const val MUSIC_FOLDER_NAME = "Music"
private const val TEST_FILENAME = "1741_(The_Battle_of_Cartagena).mp3"