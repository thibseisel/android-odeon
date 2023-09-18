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

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.nihilus.music.core.compose.theme.OdeonTheme

@Composable
internal fun ExcludedTrackRow(
    track: ExcludedTrackUiState,
    dismiss: () -> Unit
) {
    DismissibleRow(dismiss = dismiss) {
        TrackRow(
            track = track,
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
        )
    }
}

@Composable
private fun TrackRow(track: ExcludedTrackUiState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .weight(1f)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artistName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = rememberRelativeElapsedTime(track.excludeDate),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DismissibleRow(
    dismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberDismissState(
        positionalThreshold = { totalDistance -> totalDistance / 2 },
    )
    if (
        dismissState.isDismissed(DismissDirection.StartToEnd) ||
        dismissState.isDismissed(DismissDirection.EndToStart)
    ) {
        SideEffect {
            dismiss()
        }
    }

    SwipeToDismiss(
        state = dismissState,
        background = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
        },
        dismissContent = {
            content()
        },
    )
}

@Composable
private fun rememberRelativeElapsedTime(epochTime: Long): String {
    if (LocalView.current.isInEditMode) {
        return "5 minutes ago"
    }
    val context = LocalContext.current
    return remember(epochTime) {
        DateUtils.getRelativeTimeSpanString(context, epochTime * 1000, true).toString()
    }
}

@Composable
@Preview
private fun Preview() {
    OdeonTheme {
        ExcludedTrackRow(
            track = ExcludedTrackUiState(
                id = 42L,
                title = "1741 (The Battle of Cartagena)",
                artistName = "Alestorm",
                excludeDate = 1694432100L
            ),
            dismiss = {}
        )
    }
}
