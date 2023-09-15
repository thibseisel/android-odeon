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

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import fr.nihilus.music.core.compose.theme.OdeonTheme

@Composable
internal fun ConfirmDeleteDialog(
    deletedTrackCount: Int,
    accept: () -> Unit,
    cancel: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(
                pluralStringResource(
                    R.plurals.cleanup_confirmation_title,
                    deletedTrackCount,
                    deletedTrackCount
                )
            )
        },
        text = { Text(stringResource(R.string.cleanup_confirmation_message)) },
        onDismissRequest = cancel,
        confirmButton = {
            TextButton(onClick = accept) {
                Text(stringResource(fr.nihilus.music.core.ui.R.string.core_action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = cancel) {
                Text(stringResource(fr.nihilus.music.core.ui.R.string.core_cancel))
            }
        },
    )
}

@Composable
@Preview
private fun DialogPreview() {
    OdeonTheme {
        ConfirmDeleteDialog(
            deletedTrackCount = 6,
            accept = {},
            cancel = {}
        )
    }
}