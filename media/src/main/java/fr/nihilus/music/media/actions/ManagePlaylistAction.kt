/*
 * Copyright 2019 Thibault Seisel
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

package fr.nihilus.music.media.actions

import android.os.Bundle
import fr.nihilus.music.media.AppDispatchers
import fr.nihilus.music.media.playlists.PlaylistDao

internal class ManagePlaylistAction(
    private val playlistDao: PlaylistDao,
    private val dispatchers: AppDispatchers
) : BrowserAction {
    override suspend fun execute(parameters: Bundle?): Bundle? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}