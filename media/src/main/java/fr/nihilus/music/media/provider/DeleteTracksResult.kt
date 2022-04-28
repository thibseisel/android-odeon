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

import android.app.PendingIntent

/**
 * Describes the possible outcomes of a "delete tracks" operation.
 */
sealed class DeleteTracksResult {
    /**
     * Tracks have been successfully deleted.
     * @param count Number of tracks that have been deleted.
     * This might be less than the actual number of tracks you requested to delete,
     * if some of them did not exist.
     */
    data class Deleted(val count: Int) : DeleteTracksResult()

    /**
     * No tracks have been deleted because the app lacks required runtime permissions.
     * The operation may be re-attempted after granting them.
     * @param permission Name of the required Android runtime permission.
     */
    data class RequiresPermission(val permission: String) : DeleteTracksResult()

    /**
     * User should explicitly consent to delete the selected tracks.
     * @param intent Intent to display a system popup granting this app the permission
     * to delete tracks.
     * If the permission has been granted, there is no need to re-attempt the operation.
     */
    data class RequiresUserConsent(val intent: PendingIntent) : DeleteTracksResult()
}
