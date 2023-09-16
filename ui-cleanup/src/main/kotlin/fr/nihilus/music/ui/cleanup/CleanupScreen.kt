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

package fr.nihilus.music.ui.cleanup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.nihilus.music.core.compose.theme.OdeonTheme
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.ui.R as CoreUiR

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun CleanupScreen(
    tracks: List<CleanupState.Track>,
    selectedCount: Int,
    selectedFreedBytes: Long,
    toggleTrack: (track: CleanupState.Track) -> Unit,
    clearSelection: () -> Unit,
    deleteSelection: () -> Unit,
) {
    Scaffold(
        topBar = {
            if (selectedCount > 0) {
                ActionModeBar(
                    selectedCount = selectedCount,
                    freedBytes = selectedFreedBytes,
                    clearSelection = clearSelection,
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.action_cleanup_title)) }
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectedCount > 0,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                FloatingActionButton(onClick = deleteSelection) {
                    Icon(
                        painterResource(CoreUiR.drawable.ui_ic_delete_24dp),
                        contentDescription = stringResource(R.string.desc_delete_selected_tracks)
                    )
                }
            }
        }
    ) { contentPadding ->
        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.padding(contentPadding)
        ) {
            itemsIndexed(tracks, key = { _, track -> track.id.encoded }) { index, track ->
                TrackRow(
                    track = track,
                    toggle = { toggleTrack(track) }
                )
                if (index < tracks.lastIndex) {
                    Divider()
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ActionModeBar(
    selectedCount: Int,
    freedBytes: Long,
    clearSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column(Modifier.padding(start = 16.dp)) {
                Text(
                    style = MaterialTheme.typography.titleMedium,
                    text = pluralStringResource(
                        R.plurals.number_of_selected_tracks,
                        count = selectedCount,
                        selectedCount
                    ),
                )

                val formattedFreedBytes = remember(freedBytes) {
                    formatToHumanReadableByteCount(freedBytes)
                }
                Text(
                    style = MaterialTheme.typography.bodySmall,
                    text = formattedFreedBytes,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = clearSelection) {
                Icon(
                    painterResource(CoreUiR.drawable.ui_ic_clear_24dp),
                    contentDescription = stringResource(
                        R.string.desc_delete_selected_tracks
                    )
                )
            }
        }
    )
}

@Composable
@Preview
private fun ScreenPreview() {
    OdeonTheme {
        CleanupScreen(
            tracks = listOf(
                CleanupState.Track(
                    id = MediaId(MediaId.TYPE_TRACKS, category = MediaId.CATEGORY_ALL, track = 12),
                    title = "1741 (The Battle of Cartagena)",
                    fileSizeBytes = 16_340_000,
                    lastPlayedTime = null,
                    selected = false
                ),
                CleanupState.Track(
                    id = MediaId(MediaId.TYPE_TRACKS, category = MediaId.CATEGORY_ALL, track = 63),
                    title = "The 2nd Law: Isolated System",
                    fileSizeBytes = 12_763_000,
                    lastPlayedTime = null,
                    selected = true,
                ),
                CleanupState.Track(
                    id = MediaId(MediaId.TYPE_TRACKS, category = MediaId.CATEGORY_ALL, track = 871),
                    title = "Knights of Cydonia",
                    fileSizeBytes = 9_874_000,
                    lastPlayedTime = null,
                    selected = false,
                )
            ),
            selectedCount = 1,
            selectedFreedBytes = 12_763_000,
            toggleTrack = {},
            clearSelection = {},
            deleteSelection = {},
        )
    }
}
