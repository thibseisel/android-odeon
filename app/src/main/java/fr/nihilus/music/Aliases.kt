/*
 * Copyright 2017 Thibault Seisel
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

package fr.nihilus.music

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.session.MediaControllerCompat

/**
 * The result of retrieving media items to be sent to media browser clients.
 */
typealias MediaItemResult = MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>>

/**
 * A group of tasks to be executed on a [MediaControllerCompat].
 */
typealias MediaControllerRequest = (MediaControllerCompat) -> Unit

/**
 * Represents an event that is fired when the currently shown screen changes.
 * This accepts an [Int] parameter giving hints on the new location.
 */
typealias RouteChangeListener = (Int) -> Unit