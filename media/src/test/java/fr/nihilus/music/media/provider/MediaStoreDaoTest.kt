/*
 * Copyright 2021 Thibault Seisel
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
import android.os.Build
import android.provider.MediaStore.Audio.*
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import fr.nihilus.music.core.context.AppDispatchers
import fr.nihilus.music.core.os.FileSystem
import fr.nihilus.music.core.permissions.PermissionRepository
import fr.nihilus.music.core.permissions.RuntimePermission
import fr.nihilus.music.core.test.coroutines.CoroutineTestRule
import fr.nihilus.music.core.test.failAssumption
import fr.nihilus.music.media.os.MediaStoreDatabase
import fr.nihilus.music.media.os.SimulatedFileSystem
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class MediaStoreDaoTest {

    @get:Rule
    val test = CoroutineTestRule()

    @MockK private lateinit var mockPermissions: PermissionRepository

    private val dispatchers = AppDispatchers(test.dispatcher)
    private lateinit var fakeMediaStore: SQLiteMediaStore

    private val fakePermissionFlow = MutableStateFlow(
        RuntimePermission(
            canReadAudioFiles = true,
            canWriteAudioFiles = true,
        )
    )

    @BeforeTest
    fun setup() {
        MockKAnnotations.init(this)
        fakeMediaStore = SQLiteMediaStore(ApplicationProvider.getApplicationContext())
        every { mockPermissions.permissions } returns fakePermissionFlow
    }

    @AfterTest
    fun releaseDatabase() {
        fakeMediaStore.release()
    }

    @Test
    fun `Given denied read permission, when collecting any flow then returns empty list`() = test {
        fakePermissionFlow.value = RuntimePermission(
            canReadAudioFiles = false,
            canWriteAudioFiles = false,
        )

        val dao = MediaDao()

        dao.tracks.first().shouldBeEmpty()
        dao.albums.first().shouldBeEmpty()
        dao.artists.first().shouldBeEmpty()
    }

    @Test
    fun `Given failing MediaStore, when collecting any flow then emit an empty list`() = test {
        val failingDao = MediaDao(store = FailingMediaStore)

        failingDao.tracks.first().shouldBeEmpty()
        failingDao.albums.first().shouldBeEmpty()
        failingDao.artists.first().shouldBeEmpty()
    }

    @Test
    fun `When querying tracks, then list them all in alphabetic order`() = test {
        val dao = MediaDao()
        val tracks = dao.tracks.first()

        val trackIds = tracks.map { it.id }
        trackIds.shouldContainExactly(
            161L,
            309L,
            865L,
            481L,
            48L,
            125L,
            294L,
            219L,
            75L,
            464L,
            477L
        )
    }

    @Test
    fun `When collecting tracks, then map cursor columns to a list of Track`() = test {
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
    fun `When querying tracks, then its album art uri is that of its album`() = test {
        val dao = MediaDao()
        val tracks = dao.tracks.first().takeIf { it.isNotEmpty() }
            ?: failAssumption("Expected to have tracks but was empty.")

        val firstTrack = tracks.first()
        firstTrack.albumArtUri shouldBe "content://media/external/audio/albumart/65"
    }

    @Test
    fun `When querying albums, then list them all in alphabetic order`() = test {
        val dao = MediaDao()

        val albums = dao.albums.first()
        val albumIds = albums.map { it.id }

        albumIds.shouldContainExactly(40L, 38L, 102L, 95L, 7L, 6L, 98L, 65L, 26L)
    }

    @Test
    fun `When querying albums, then map cursor columns to a list of Album`() = test {
        val dao = MediaDao()
        val albums = dao.albums.first()

        albums.find { it.id == 40L }?.should { secondLaw ->
            secondLaw.id shouldBe 40L
            secondLaw.title shouldBe "The 2nd Law"
            secondLaw.artistId shouldBe 18L
            secondLaw.artist shouldBe "Muse"
            secondLaw.releaseYear shouldBe 2012
            secondLaw.trackCount shouldBe 1
            secondLaw.albumArtUri shouldBe "content://media/external/audio/albumart/40"
        } ?: failAssumption("Expected the album \"The 2nd Law\".")
    }

    @Test
    fun `When querying artists, then list them all in alphabetic order`() = test {
        val dao = MediaDao()

        val artists = dao.artists.first()
        val artistIds = artists.map { it.id }

        artistIds.shouldContainExactly(5L, 26L, 4L, 13L, 18L)
    }

    @Test
    fun `When querying artists, then map cursor columns to a list of Artist`() = test {
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
    fun `When querying artists, then its icon should be that of its most recent album`() = test {
        val dao = MediaDao()
        val artists = dao.artists.first()

        // Alestorm only have one album here ; its icon should be that of that album
        val alestorm =
            artists.find { it.id == 26L } ?: failAssumption("Alestorm is missing (id = 26)")
        alestorm.iconUri shouldBe "content://media/external/audio/albumart/65"

        // Foo Fighters have 3 albums, use the icon of "Concrete and Gold"
        val fooFighters =
            artists.find { it.id == 13L } ?: failAssumption("Foo Fighters is missing (id = 13)")
        fooFighters.iconUri shouldBe "content://media/external/audio/albumart/102"
    }

    @Test
    fun `While collecting from any flow, then register ContentObserver for each`() = test {
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
    fun `Given any flow, when content changed then reload media list`() = test {
        val dao = MediaDao()

        flowShouldEmitWheneverContentChanges(dao.tracks)
        flowShouldEmitWheneverContentChanges(dao.albums)
        flowShouldEmitWheneverContentChanges(dao.artists)
    }

    private suspend fun TestScope.flowShouldEmitWheneverContentChanges(flow: Flow<List<*>>) =
        flow.drop(1).test {
            // Simulate a slight delay before triggering change
            testScheduler.runCurrent()

            val registeredObservers = fakeMediaStore.observers.takeUnless { it.isEmpty() }
                ?: failAssumption("Assumed at least one ContentObserver to be registered.")
            registeredObservers.forEach { it.observer.onChange(false, null) }

            awaitItem()
            expectNoEvents()
        }

    @Test
    //@Ignore("Test doesn't pass due to being unable to cancel a running query")
    fun `Given any flow, when content changed multiple time quickly then only emit once`() = test {
        val dao = MediaDao()

        shouldConflateFlowConsecutiveChanges(dao.tracks)
        shouldConflateFlowConsecutiveChanges(dao.albums)
        shouldConflateFlowConsecutiveChanges(dao.artists)
    }

    private suspend fun TestScope.shouldConflateFlowConsecutiveChanges(flow: Flow<List<*>>) =
        flow.drop(1).test {
            testScheduler.runCurrent()

            val registeredObservers = fakeMediaStore.observers.takeUnless { it.isEmpty() }
                ?: failAssumption("Assumed at least one ContentObserver to be registered.")
            repeat(2) {
                registeredObservers.forEach { it.observer.onChange(false, null) }
            }

            awaitItem()
            expectNoEvents()
        }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.Q)
    fun `Given denied permission, when deleting tracks then returns RequiresPermission`() = test {
        fakePermissionFlow.value = RuntimePermission(
            canReadAudioFiles = true,
            canWriteAudioFiles = false
        )

        val dao = MediaDao()
        val result = dao.deleteTracks(longArrayOf(161, 309))

        result.shouldBeInstanceOf<DeleteTracksResult.RequiresPermission>()
        result.permission shouldBe Manifest.permission.WRITE_EXTERNAL_STORAGE
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.Q)
    fun `When deleting a track, then also delete the corresponding file`() = test {
        val simulatedFileSystem = SimulatedFileSystem("$MUSIC_FOLDER_NAME/$TEST_FILENAME")
        val dao = MediaDao(fs = simulatedFileSystem)

        dao.deleteTracks(longArrayOf(161L))

        simulatedFileSystem.fileExists("$MUSIC_FOLDER_NAME/$TEST_FILENAME") shouldBe false
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.Q)
    fun `When a track file is deleted, then delete its metadata from MediaStore`() = test {
        val dao = MediaDao(
            fs = SimulatedFileSystem("$MUSIC_FOLDER_NAME/$TEST_FILENAME")
        )

        dao.deleteTracks(longArrayOf(161L))

        fakeMediaStore.mediaExists(161L) shouldBe false
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.Q)
    fun `When a track file cannot be deleted, then do not delete its metadata from MediaStore`() =
        test {
            // Files doesn't exists, so deleting them will fail.
            val dao = MediaDao(fs = SimulatedFileSystem())

            dao.deleteTracks(longArrayOf(161L))

            fakeMediaStore.mediaExists(161L) shouldBe true
        }

    private fun MediaDao(
        store: MediaStoreDatabase = fakeMediaStore,
        fs: FileSystem = SimulatedFileSystem(),
    ) = MediaStoreDao(store, fs, mockPermissions, dispatchers)
}

private const val MUSIC_FOLDER_NAME = "Music"
private const val TEST_FILENAME = "1741_(The_Battle_of_Cartagena).mp3"
