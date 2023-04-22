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

package fr.nihilus.music.core.ui.actions

import android.Manifest
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ALBUMS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ARTISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_PLAYLISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.media.tracks.DeleteTracksResult
import fr.nihilus.music.media.tracks.TrackRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Verify behavior of [DeleteTracksAction].
 */
internal class DeleteTracksActionTest {

    @MockK private lateinit var mockRepository: TrackRepository
    private lateinit var deleteTracks: DeleteTracksAction

    @BeforeTest
    fun setup() {
        MockKAnnotations.init(this)
        deleteTracks = DeleteTracksAction(mockRepository)
    }

    @Test
    fun `Given invalid track media ids, when deleting then fail with IAE`() = runTest {
        val invalidTrackIds = listOf(
            MediaId(TYPE_TRACKS, CATEGORY_ALL),
            MediaId(TYPE_ALBUMS, "13"),
            MediaId(TYPE_ARTISTS, "78"),
            MediaId(TYPE_PLAYLISTS, "9")
        )

        for (mediaId in invalidTrackIds) {
            shouldThrow<IllegalArgumentException> {
                deleteTracks(listOf(mediaId))
            }
        }

        confirmVerified(mockRepository)
    }

    @Test
    fun `When deleting tracks then remove records from dao`() = runTest {
        coEvery { mockRepository.deleteTracks(any()) } returns DeleteTracksResult.Deleted(3)

        val deleteResult = deleteTracks(
            mediaIds = listOf(
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 161),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 48),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 75)
            )
        )

        deleteResult.shouldBeInstanceOf<DeleteTracksResult.Deleted>()
        deleteResult.count shouldBe 3

        coVerifySequence {
            mockRepository.deleteTracks(longArrayOf(161, 48, 75))
        }
    }

    @Test
    fun `Given denied permission, when deleting tracks then return RequiresPermission`() = runTest {
        coEvery { mockRepository.deleteTracks(any()) } returns DeleteTracksResult.RequiresPermission(
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val result = deleteTracks(
            mediaIds = listOf(
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 161),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 464)
            )
        )

        result.shouldBeInstanceOf<DeleteTracksResult.RequiresPermission>()
        result.permission shouldBe Manifest.permission.WRITE_EXTERNAL_STORAGE

        coVerifySequence {
            mockRepository.deleteTracks(longArrayOf(161, 464))
        }
    }
}
