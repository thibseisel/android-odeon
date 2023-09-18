/*
 * Copyright 2023 Thibault Seisel
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

package fr.nihilus.music.ui.settings.exclusion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.nihilus.music.core.compose.theme.OdeonTheme
import fr.nihilus.music.ui.settings.R

@Composable
internal fun ExcludedTracksScreen(
    tracks: List<ExcludedTrackUiState>,
    restoreTrack: (ExcludedTrackUiState) -> Unit
) {
    Column {
        FeatureNotice()
        TrackList(tracks, dismissTrack = { restoreTrack(it) })
    }
}

@Composable
private fun FeatureNotice() {
    Row {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier.padding(16.dp)
        )
        Text(
            text = stringResource(R.string.track_exclusion_feature_description),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 16.dp)
        )
    }
}

@Composable
private fun TrackList(
    tracks: List<ExcludedTrackUiState>,
    dismissTrack: (ExcludedTrackUiState) -> Unit
) {
    LazyColumn {
        itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
            ExcludedTrackRow(
                track = track,
                dismiss = { dismissTrack(track) }
            )
            if (index < tracks.lastIndex) {
                Divider()
            }
        }
    }
}

@Composable
@Preview
private fun Preview() {
    OdeonTheme {
        ExcludedTracksScreen(
            tracks = listOf(
                ExcludedTrackUiState(
                    id = 42L,
                    title = "1741 (The Battle of Cartagena)",
                    artistName = "Alestorm",
                    excludeDate = 1694432100L
                ),
                ExcludedTrackUiState(
                    id = 64,
                    title = "All These Things I Hate (Revolve Around Me)",
                    artistName = "Bullet For My Valentine",
                    excludeDate = 1669203000L
                )
            ),
            restoreTrack = {},
        )
    }
}
