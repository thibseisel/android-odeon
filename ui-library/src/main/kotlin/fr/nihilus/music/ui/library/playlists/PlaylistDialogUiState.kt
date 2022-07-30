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

package fr.nihilus.music.ui.library.playlists

import android.net.Uri
import fr.nihilus.music.core.media.MediaId

/**
 * UI state of the "Add to playlist" dialog.
 */
internal data class PlaylistDialogUiState(
    /**
     * List of all user-defined playlists.
     * These are only a subset of those displayed on the "playlists" screen.
     */
    val playlists: List<Playlist>
) {
    /**
     * Simple set of metadata of a playlist.
     */
    data class Playlist(
        /**
         * Unique identifier of the playlist in the media tree.
         */
        val id: MediaId,
        /**
         * Title given to the playlist by the user.
         */
        val title: String,
        /**
         * URI pointing to an icon illustrating the playlist.
         */
        val iconUri: Uri?,
    )
}
