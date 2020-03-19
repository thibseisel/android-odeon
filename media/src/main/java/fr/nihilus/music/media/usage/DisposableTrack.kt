/*
 * Copyright 2020 Thibault Seisel
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

package fr.nihilus.music.media.usage

/**
 * Information on a track that could be deleted from the device's storage to free-up space.
 *
 * @property trackId Unique identifier of the related track.
 * @property title The display title of the related track.
 * @property fileSizeBytes The size of the file stored on the device's storage in bytes.
 * @property lastPlayedTime The epoch time at which that track has been played for the last time,
 * or `null` if it has never been played.
 */
class DisposableTrack(
    val trackId: Long,
    val title: String,
    val fileSizeBytes: Long,
    val lastPlayedTime: Long?
)