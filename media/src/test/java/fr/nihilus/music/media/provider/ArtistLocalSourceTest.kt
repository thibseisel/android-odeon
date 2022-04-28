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

import android.database.ContentObserver
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Artists
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import fr.nihilus.music.core.context.AppDispatchers
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
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
internal class ArtistLocalSourceTest {
    @get:Rule val test = CoroutineTestRule()
    @get:Rule val resolverRule = ContentResolverTestRule()

    @MockK private lateinit var mockPermissions: PermissionRepository
    @SpyK private var resolver = resolverRule.resolver

    private lateinit var localArtists: ArtistLocalSource

    @BeforeTest
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { mockPermissions.permissions } returns MutableStateFlow(
            RuntimePermission(canReadAudioFiles = true, canWriteAudioFiles = true)
        )

        resolverRule.registerProvider(MediaStore.AUTHORITY, FakeAudioMediaProvider::class.java)

        localArtists = ArtistLocalSource(
            resolver = resolver,
            permissionRepository = mockPermissions,
            dispatchers = AppDispatchers(test.dispatcher)
        )
    }

    @Test
    fun `artists - returns all artists sorted alphabetically`() = test {
        val artists = localArtists.artists.first()

        extracting(artists, Artist::name).shouldContainExactly(
            "AC/DC",
            "Alestorm",
            "Avenged Sevenfold",
            "Foo Fighters",
            "Muse",
        )
    }

    @Test
    fun `artists - reads artists from MediaStore`() = test {
        val artists = localArtists.artists.first()

        artists.forOne {
            it shouldBe Artist(
                id = 5,
                name = "AC/DC",
                albumCount = 1,
                trackCount = 2,
                iconUri = "content://media/external/audio/albumart/7"
            )
        }
    }

    @Test
    fun `artists - icon should be that of their most recent album`() = test {
        val artists = localArtists.artists.first()

        withClue("Alestorm only have 1 album, its icon should be that album's artwork") {
            artists.forOne {
                it.name shouldBe "Alestorm"
                it.iconUri shouldBe "content://media/external/audio/albumart/65"
            }
        }

        withClue("Foo Fighters have 3 albums, its icon should be the artwork of \"Concrete and Gold\"") {
            artists.forOne {
                it.name shouldBe "Foo Fighters"
                it.iconUri shouldBe "content://media/external/audio/albumart/102"
            }
        }
    }

    @Test
    fun `artists - emits whenever artists change`() = test {
        val observerSlot = slot<ContentObserver>()
        every {
            resolver.registerContentObserver(
                any(),
                any(),
                capture(observerSlot)
            )
        } answers { callOriginal() }

        localArtists.artists.drop(1).test {
            // Change on root URI should trigger update
            resolver.notifyChange(Artists.EXTERNAL_CONTENT_URI, null)
            awaitItem()

            // Change on ID URI should trigger update
            resolver.notifyChange(Artists.EXTERNAL_CONTENT_URI.withAppendedId(5), null)
            awaitItem()

            // Change on unrelated URI should not trigger update
            resolver.notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null)
            expectNoEvents()
        }
    }

    @Test
    fun `artists - returns empty list when read permission is denied`() = test {
        every { mockPermissions.permissions } returns MutableStateFlow(
            RuntimePermission(
                canReadAudioFiles = false,
                canWriteAudioFiles = false
            )
        )

        localArtists.artists.first().shouldBeEmpty()
    }

    @Test
    fun `tracks - emits artists list whenever read permission gets granted`() = test {
        val livePermissions = MutableStateFlow(
            RuntimePermission(canReadAudioFiles = false, canWriteAudioFiles = false)
        )
        every { mockPermissions.permissions } returns livePermissions

        localArtists.artists.drop(1).test {
            livePermissions.value = RuntimePermission(
                canReadAudioFiles = true,
                canWriteAudioFiles = false,
            )
            awaitItem().shouldNotBeEmpty()
            expectNoEvents()
        }
    }
}
