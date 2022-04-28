/*
 * Copyright 2022 Thibault Seisel
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
import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Albums
import android.provider.MediaStore.Audio.Media
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import fr.nihilus.music.core.context.AppDispatchers
import fr.nihilus.music.core.os.FileSystem
import fr.nihilus.music.core.permissions.PermissionRepository
import fr.nihilus.music.core.permissions.RuntimePermission
import fr.nihilus.music.core.test.coroutines.CoroutineTestRule
import io.kotest.assertions.extracting
import io.kotest.assertions.withClue
import io.kotest.inspectors.forOne
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
internal class TrackLocalSourceTest {
    @get:Rule val test = CoroutineTestRule()
    @get:Rule val resolverRule = ContentResolverTestRule()

    @MockK private lateinit var mockPermissions: PermissionRepository
    @MockK private lateinit var mockFiles: FileSystem
    @SpyK private var resolver: ContentResolver = resolverRule.resolver

    private lateinit var localTracks: TrackLocalSource

    @BeforeTest
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { mockPermissions.permissions } returns MutableStateFlow(
            RuntimePermission(canReadAudioFiles = true, canWriteAudioFiles = true)
        )
        localTracks = TrackLocalSource(
            resolver = resolver,
            fileSystem = mockFiles,
            permissionRepository = mockPermissions,
            dispatchers = AppDispatchers(test.dispatcher)
        )
    }

    @Test
    fun `tracks - returns all tracks sorted alphabetically`() = test {
        resolverRule.registerProvider(MediaStore.AUTHORITY, FakeAudioMediaProvider::class.java)

        val tracks = localTracks.tracks.first()

        extracting(tracks, Track::title).shouldContainExactly(
            "1741 (The Battle of Cartagena)",
            "The 2nd Law: Isolated System",
            "Algorithm",
            "Dirty Water",
            "Give It Up",
            "Jailbreak",
            "Knights of Cydonia",
            "A Matter of Time",
            "Nightmare",
            "The Pretenders",
            "Run"
        )
    }

    @Test
    fun `tracks - reads tracks from MediaStore`() = test {
        resolverRule.registerProvider(MediaStore.AUTHORITY, FakeAudioMediaProvider::class.java)

        val tracks = localTracks.tracks.first()

        tracks.forOne {
            it.id shouldBe 161L
            it.title shouldBe "1741 (The Battle of Cartagena)"
            it.albumId shouldBe 65
            it.album shouldBe "Sunset on the Golden Age"
            it.artistId shouldBe 26L
            it.artist shouldBe "Alestorm"
            it.discNumber shouldBe 1
            it.trackNumber shouldBe 4
            it.duration shouldBe 437603L
            it.availabilityDate shouldBe 1466283480L
            it.fileSize shouldBe 17_506_481L
            it.mediaUri shouldBe "${Media.EXTERNAL_CONTENT_URI}/161"
            withClue("Track artwork uri should be that of its album") {
                it.albumArtUri shouldBe "content://media/external/audio/albumart/65"
            }
        }

        tracks.forOne {
            it.id shouldBe 125L
            it.title shouldBe "Jailbreak"
            it.albumId shouldBe 7L
            it.album shouldBe "Greatest Hits 30 Anniversary Edition"
            it.artistId shouldBe 5L
            it.artist shouldBe "AC/DC"
            it.discNumber shouldBe 2
            it.trackNumber shouldBe 14
            it.duration shouldBe 276668L
            it.availabilityDate shouldBe 1455310140L
            it.fileSize shouldBe 6_750_404L
            it.mediaUri shouldBe "${Media.EXTERNAL_CONTENT_URI}/125"
            withClue("Track artwork uri should be that of its album") {
                it.albumArtUri shouldBe "content://media/external/audio/albumart/7"
            }
        }
    }

    @Test
    fun `tracks - should emit whenever content changes`() = test {
        val observerSlot = slot<ContentObserver>()
        every {
            resolver.registerContentObserver(
                any(),
                any(),
                capture(observerSlot)
            )
        } answers { callOriginal() }
        resolverRule.registerProvider(MediaStore.AUTHORITY, FakeAudioMediaProvider::class.java)

        localTracks.tracks.drop(1).test {
            resolverRule.resolver.notifyChange(Media.EXTERNAL_CONTENT_URI, null)
            awaitItem()

            resolverRule.resolver.notifyChange(
                Media.EXTERNAL_CONTENT_URI.withAppendedId(161L),
                null
            )
            awaitItem()

            resolverRule.resolver.notifyChange(Albums.EXTERNAL_CONTENT_URI, null)
            expectNoEvents()
        }

        verify(exactly = 1) {
            resolver.unregisterContentObserver(refEq(observerSlot.captured))
        }
    }

    @Test
    fun `tracks - returns empty list when read permission is denied`() = test {
        every { mockPermissions.permissions } returns MutableStateFlow(
            value = RuntimePermission(canReadAudioFiles = false, canWriteAudioFiles = false)
        )

        val tracks = localTracks.tracks.first()
        tracks.shouldBeEmpty()
    }

    @Test
    fun `tracks - emits track list whenever read permission gets granted`() = test {
        resolverRule.registerProvider(MediaStore.AUTHORITY, FakeAudioMediaProvider::class.java)
        val livePermissions = MutableStateFlow(
            RuntimePermission(canReadAudioFiles = false, canWriteAudioFiles = false)
        )
        every { mockPermissions.permissions } returns livePermissions

        localTracks.tracks.drop(1).test {
            livePermissions.value = RuntimePermission(
                canReadAudioFiles = true,
                canWriteAudioFiles = false,
            )
            awaitItem().shouldNotBeEmpty()
            expectNoEvents()
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P, Build.VERSION_CODES.Q])
    fun `deleteTracks - deletes tracks from MediaStore`() = test {
        every { mockFiles.deleteFile(any()) } returns true
        resolverRule.registerProvider(MediaStore.AUTHORITY, FakeAudioMediaProvider::class.java)

        val result = localTracks.deleteTracks(longArrayOf(161, 309))

        result shouldBe DeleteTracksResult.Deleted(2)
        verifyAll {
            mockFiles.deleteFile("Music/1741_(The_Battle_of_Cartagena).mp3")
            mockFiles.deleteFile("Music/The_2nd_Law_(Isolated_System).mp3")
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P, Build.VERSION_CODES.Q])
    fun `deleteTracks - returns RequiresPermission when permission is denied`() = test.run {
        every { mockPermissions.permissions } returns MutableStateFlow(
            value = RuntimePermission(canReadAudioFiles = true, canWriteAudioFiles = false)
        )

        val result = localTracks.deleteTracks(longArrayOf(CARTAGENA.id, ISOLATED_SYSTEM.id))

        result shouldBe DeleteTracksResult.RequiresPermission(
            permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        confirmVerified(mockFiles)
    }

    @Test
    fun `deleteTracks - returns intent when scoped storage is enabled`() = test {
        resolverRule.registerProvider(MediaStore.AUTHORITY, FakeAudioMediaProvider::class.java)
        val result = localTracks.deleteTracks(
            longArrayOf(161L, 309L)
        )

        result.shouldBeInstanceOf<DeleteTracksResult.RequiresUserConsent>()
        confirmVerified(mockFiles)
    }
}
