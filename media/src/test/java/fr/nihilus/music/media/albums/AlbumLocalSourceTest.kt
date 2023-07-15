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

package fr.nihilus.music.media.albums

import android.content.ContentResolver
import android.database.ContentObserver
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Albums
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import fr.nihilus.music.core.context.AppDispatchers
import fr.nihilus.music.core.permissions.PermissionRepository
import fr.nihilus.music.core.permissions.RuntimePermission
import fr.nihilus.music.core.test.coroutines.CoroutineTestRule
import fr.nihilus.music.media.provider.ContentResolverTestRule
import fr.nihilus.music.media.provider.FakeAudioMediaProvider
import fr.nihilus.music.media.provider.withAppendedId
import io.kotest.assertions.extracting
import io.kotest.inspectors.forOne
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
internal class AlbumLocalSourceTest {

    @get:Rule
    val test = CoroutineTestRule()

    @get:Rule
    val resolverRule = ContentResolverTestRule()

    @MockK private lateinit var mockPermissions: PermissionRepository
    @SpyK var resolver: ContentResolver = resolverRule.resolver

    private lateinit var localAlbums: AlbumLocalSource

    @BeforeTest
    fun setup() {
        resolverRule.registerProvider(MediaStore.AUTHORITY, FakeAudioMediaProvider::class.java)
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { mockPermissions.permissions } returns MutableStateFlow(
            RuntimePermission(canReadAudioFiles = true, canWriteAudioFiles = false)
        )
        localAlbums = AlbumLocalSource(
            resolver = resolver,
            permissionRepository = mockPermissions,
            dispatchers = AppDispatchers(test.dispatcher)
        )
    }

    @Test
    fun `albums - returns all albums sorted alphabetically`() = test {
        val albums = localAlbums.albums.first()

        extracting(albums, Album::title).shouldContainExactly(
            "The 2nd Law",
            "Black Holes and Revelations",
            "Concrete and Gold",
            "Echoes, Silence, Patience & Grace",
            "Greatest Hits Anniversary Edition",
            "Nightmare",
            "Simulation Theory",
            "Sunset on the Golden Age",
            "Wasting Light"
        )
    }

    @Test
    fun `albums - reads albums from MediaStore`() = test {
        val albums = localAlbums.albums.first()

        albums.forOne {
            it shouldBe Album(
                id = 40,
                title = "The 2nd Law",
                artistId = 18,
                artist = "Muse",
                releaseYear = 2012,
                trackCount = 1,
                albumArtUri = "content://media/external/audio/albumart/40"
            )
        }
    }

    @Test
    fun `albums - should emit whenever content changes`() = test {
        val observerSlot = slot<ContentObserver>()
        every {
            resolver.registerContentObserver(
                any(),
                any(),
                capture(observerSlot)
            )
        } answers { callOriginal() }

        localAlbums.albums.drop(1).test {
            resolver.notifyChange(Albums.EXTERNAL_CONTENT_URI, null)
            awaitItem()

            resolver.notifyChange(Albums.EXTERNAL_CONTENT_URI.withAppendedId(40), null)
            awaitItem()

            resolver.notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null)
            expectNoEvents()
        }

        verify(exactly = 1) {
            resolver.registerContentObserver(Albums.EXTERNAL_CONTENT_URI, true, any())
            resolver.unregisterContentObserver(refEq(observerSlot.captured))
        }
    }

    @Test
    fun `albums - returns empty list when read permission is denied`() = test {
        every { mockPermissions.permissions } returns MutableStateFlow(
            RuntimePermission(
                canReadAudioFiles = false,
                canWriteAudioFiles = false
            )
        )

        localAlbums.albums.first().shouldBeEmpty()
    }

    @Test
    fun `tracks - emits albums list whenever read permission gets granted`() = test {
        val livePermissions = MutableStateFlow(
            RuntimePermission(canReadAudioFiles = false, canWriteAudioFiles = false)
        )
        every { mockPermissions.permissions } returns livePermissions

        localAlbums.albums.drop(1).test {
            livePermissions.value = RuntimePermission(
                canReadAudioFiles = true,
                canWriteAudioFiles = false,
            )
            awaitItem().shouldNotBeEmpty()
            expectNoEvents()
        }
    }
}
