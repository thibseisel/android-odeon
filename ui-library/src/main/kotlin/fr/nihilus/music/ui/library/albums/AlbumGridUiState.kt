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

package fr.nihilus.music.ui.library.albums

import android.net.Uri
import fr.nihilus.music.core.media.MediaId

/**
 * UI state of [AlbumsFragment].
 */
internal data class AlbumGridUiState(
    /**
     * List of all albums in the music library.
     * Sorted alphabetically.
     */
    val albums: List<AlbumUiState>,
    /**
     * Whether the list of albums is currently being loaded.
     */
    val isLoadingAlbums: Boolean
)

/**
 * UI representation of an album from the music library.
 */
internal data class AlbumUiState(
    /**
     * Unique identifier of the album.
     * Use it to reference this album in various operations.
     */
    val id: MediaId,
    /**
     * Title given to this album by its producer.
     */
    val title: String,
    /**
     * Name of the artist that produced this album.
     */
    val artist: String,
    /**
     * Optional URI pointing to the album cover artwork.
     */
    val artworkUri: Uri?,
)
