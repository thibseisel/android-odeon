/*
 * Copyright 2018 Thibault Seisel
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

package fr.nihilus.music.client

import fr.nihilus.music.base.BaseViewModel

/**
 * A ViewModel that connects to this application's media browser through [MediaBrowserConnection]
 * when it is initiated and disconnects from it when destroyed.
 *
 * You should extend this class if you intend to browse available media
 * or send commands to the media session transport controls.
 *
 * @constructor Initiate a connection to the media browser using the given [connection].
 * @param connection The connection to initiate between this ViewModel and the media browser.
 */
@Deprecated("This base class does not help that much. " +
        "Inject MediaBrowserConnection into ViewModels. " +
        "ViewModels linked to the parent activity can establish and dispose the connection.")
abstract class MediaBrowserViewModel(
    protected val connection: MediaBrowserConnection
) : BaseViewModel() {
    private val token = MediaBrowserConnection.ClientToken()

    init {
        connection.connect(token)
    }

    override fun onCleared() {
        connection.disconnect(token)
        super.onCleared()
    }
}