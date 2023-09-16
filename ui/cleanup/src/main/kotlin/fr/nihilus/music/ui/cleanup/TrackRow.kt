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

import android.text.format.DateUtils
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.nihilus.music.core.compose.theme.OdeonTheme
import fr.nihilus.music.core.media.MediaId

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun TrackRow(track: CleanupState.Track, toggle: () -> Unit) {
    val itemBackgroundColor by animateColorAsState(
        label = "Item background color",
        targetValue = if (track.selected)
            MaterialTheme.colorScheme.secondaryContainer
        else
            MaterialTheme.colorScheme.background
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .toggleable(
                value = track.selected,
                role = Role.Checkbox,
                onValueChange = { toggle() },
            )
            .background(itemBackgroundColor)
            .padding(horizontal = 16.dp)
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
            Checkbox(
                checked = track.selected,
                onCheckedChange = null,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Column(
            modifier = Modifier
                .padding(start = 32.dp, end = 16.dp)
                .weight(1f)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.paddingFromBaseline(top = 32.dp)
            )

            val lastPlayedTime = if (track.lastPlayedTime == null) {
                stringResource(R.string.never_played)
            } else {
                val elapsedTime = rememberElapsedTimeFrom(epochTime = track.lastPlayedTime)
                stringResource(R.string.last_played_description, elapsedTime)
            }
            Text(
                text = lastPlayedTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.paddingFromBaseline(top = 16.dp),
            )
        }

        val formattedFileSize = remember(track.fileSizeBytes) {
            formatToHumanReadableByteCount(track.fileSizeBytes)
        }
        Text(
            text = formattedFileSize,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.paddingFromBaseline(top = 28.dp)
        )
    }
}

@Composable
private fun rememberElapsedTimeFrom(epochTime: Long): String {
    if (LocalView.current.isInEditMode) {
        return "3 months ago"
    }
    val context = LocalContext.current
    return remember(epochTime) {
        DateUtils.getRelativeTimeSpanString(context, epochTime * 1000, true).toString()
    }
}

@Composable
@Preview
private fun RowPreview() {
    var selected by remember { mutableStateOf(true) }

    OdeonTheme {
        TrackRow(
            track = CleanupState.Track(
                id = MediaId(
                    MediaId.TYPE_TRACKS,
                    MediaId.CATEGORY_ALL,
                    42
                ),
                title = "All These Things I Hate (Revolve Around Me)",
                fileSizeBytes = 9461760L,
                lastPlayedTime = 1694521200L,
                selected = selected
            ),
            toggle = { selected = !selected }
        )
    }
}
