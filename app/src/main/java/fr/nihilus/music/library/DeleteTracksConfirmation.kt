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

package fr.nihilus.music.library

import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.media.provider.DeleteTracksResult

/**
 * Wraps the result of deleting a single track.
 * Note that while the main outcome of a delete operation is that the track is deleted,
 * this is not necessarily the case. You may need to let user grant permissions to proceed.
 */
class DeleteTracksConfirmation(
    /**
     * identifier of the target audio track.
     */
    val trackId: MediaId,
    /**
     * Result of deleting that track.
     */
    val result: DeleteTracksResult,
)
