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

package fr.nihilus.music.ui.library.artists.detail

import android.net.Uri
import fr.nihilus.music.core.media.MediaId
import kotlin.time.Duration

internal data class ArtistDetailUiState(
    val name: String,
    val albums: List<ArtistAlbumUiState>,
    val tracks: List<ArtistTrackUiState>,
    val isLoading: Boolean,
)

internal data class ArtistAlbumUiState(
    val id: MediaId,
    val title: String,
    val trackCount: Int,
    val artworkUri: Uri?,
)

internal data class ArtistTrackUiState(
    val id: MediaId,
    val title: String,
    val duration: Duration,
    val iconUri: Uri?,
)
