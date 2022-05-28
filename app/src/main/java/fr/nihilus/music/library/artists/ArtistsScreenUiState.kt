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

package fr.nihilus.music.library.artists

import android.net.Uri
import fr.nihilus.music.core.media.MediaId

internal data class ArtistsScreenUiState(
    /**
     * List of all artists that contributed to tracks in the music library.
     * Sorted alphabetically by name.
     */
    val artists: List<ArtistUiState>,
    /**
     * Whether the list of artists is currently being loaded.
     */
    val isLoadingArtists: Boolean
)

/**
 * UI representation of an artist.
 */
internal data class ArtistUiState(
    /**
     * Unique identifier of the artist.
     * Use it to reference this artist in various operations.
     */
    val id: MediaId,
    /**
     * Name given to this artist.
     */
    val name: String,
    /**
     * Number of tracks in the music library that have been produced by this artist.
     */
    val trackCount: Int,
    /**
     * Optional URI pointing to an illustration of this artist.
     */
    val iconUri: Uri?,
)
