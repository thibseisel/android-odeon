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

import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.os.PermissionDeniedException
import fr.nihilus.music.media.provider.MediaDao
import javax.inject.Inject

/**
 * Handler for actions related to deleting tracks from the device's storage.
 *
 * @property mediaDao Handle to the shared media storage.
 */
class DeleteTracksAction @Inject constructor(
    private val mediaDao: MediaDao
) {

    /**
     * Deletes tracks from the device's shared storage, making them definitely
     * unavailable to other applications.
     *
     * @param mediaIds The media ids of tracks that should be deleted.
     * Those should be valid track media ids, i.e. given any [type][MediaId.type]
     * or [category][MediaId.category], the [track identifier][MediaId.track] should not be `null`.
     *
     * @throws PermissionDeniedException If the user has not granted permission to write
     * to the device's external storage.
     */
    suspend fun delete(mediaIds: List<MediaId>): Int {
        val trackIds = LongArray(mediaIds.size) {
            val mediaId = mediaIds[it]
            requireNotNull(mediaId.track) { "Invalid track media id: $mediaId" }
        }

        return mediaDao.deleteTracks(trackIds)
    }
}