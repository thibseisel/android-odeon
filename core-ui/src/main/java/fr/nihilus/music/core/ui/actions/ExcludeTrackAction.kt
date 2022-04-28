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

package fr.nihilus.music.core.ui.actions

import fr.nihilus.music.core.database.exclusion.TrackExclusion
import fr.nihilus.music.core.database.exclusion.TrackExclusionDao
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.os.Clock
import javax.inject.Inject

/**
 * An action to exclude tracks from the music library.
 * A track mark as excluded is no longer part of media collections.
 */
class ExcludeTrackAction @Inject constructor(
    private val excludeList: TrackExclusionDao,
    private val clock: Clock
) {
    suspend operator fun invoke(trackMediaId: MediaId) {
        val trackId = requireNotNull(trackMediaId.track) {
            "Attempt to exclude a media that's not a track: $trackMediaId"
        }
        excludeList.exclude(TrackExclusion(trackId, clock.currentEpochTime))
    }
}
