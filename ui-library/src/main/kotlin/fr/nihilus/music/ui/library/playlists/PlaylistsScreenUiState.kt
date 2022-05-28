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

internal data class PlaylistsScreenUiState(
    /**
     * List of all playlists available in the application.
     */
    val playlists: List<PlaylistUiState>,
    /**
     * Whether the list of playlists is currently being loaded.
     */
    val isLoadingPlaylists: Boolean,
)

/**
 * UI representation of a manual or automatic collection of tracks from the music library.
 */
internal data class PlaylistUiState(
    /**
     * Unique identifier of this playlist.
     * May be used to reference this playlist in various operations.
     */
    val id: MediaId,
    /**
     * Title given to this playlist.
     */
    val title: String,
    /**
     * Additional information on this playlist, to be displayed alongside its title.
     */
    val subtitle: String,
    /**
     * Optional URI pointing to an icon illustrating this playlist.
     */
    val iconUri: Uri?,
)
