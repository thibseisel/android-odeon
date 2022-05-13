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

package fr.nihilus.music.core.ui.actions

import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ALBUMS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ARTISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.media.tracks.TrackRepository
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.MockKAnnotations
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class ExcludeTrackActionTest {

    @RelaxedMockK private lateinit var mockRepository: TrackRepository
    private lateinit var action: ExcludeTrackAction

    @BeforeTest
    fun setup() {
        MockKAnnotations.init(this)
        action = ExcludeTrackAction(mockRepository)
    }

    @Test
    fun `excludes track matching id`() = runTest {
        action(
            MediaId(TYPE_TRACKS, CATEGORY_ALL, 42L)
        )

        coVerifySequence {
            mockRepository.excludeTrack(42L)
        }
    }

    @Test
    fun `fails with IAE when media id is invalid`() = runTest {
        for (mediaId in listOf(
            MediaId(TYPE_TRACKS, CATEGORY_ALL),
            MediaId(TYPE_ALBUMS, "13"),
            MediaId(TYPE_ARTISTS, "78"),
            MediaId(TYPE_ARTISTS, "9")
        )) {
            shouldThrow<IllegalArgumentException> {
                action(mediaId)
            }
        }

        confirmVerified(mockRepository)
    }
}
