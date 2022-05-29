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

package fr.nihilus.music.ui.library.tracks

import android.app.PendingIntent
import fr.nihilus.music.core.media.MediaId

/**
 * Base class for UI events to be handled in the context of [AllTracksFragment].
 */
internal sealed class TrackEvent {
    /**
     * Dispatched to notify users that an attempt to delete a single track has been successful.
     */
    object TrackSuccessfullyDeleted : TrackEvent()

    /**
     * Dispatched when attempting to delete a single track from the music library,
     * but the app doesn't have the permission to write to external storage.
     * The delete operation should be re-attempted after the permission has been granted.
     *
     * @param trackId Track whose delete attempt failed due to missing permissions.
     */
    data class RequiresStoragePermission(val trackId: MediaId) : TrackEvent()

    /**
     * Dispatched when attempting to delete a single track from the music library and the app
     * needs to launch another activity to grant it the permission to do so.
     *
     * @param intent Intent to start an activity that proceeds with the permission request.
     */
    data class RequiresUserConsent(val intent: PendingIntent) : TrackEvent()
}
