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

package fr.nihilus.music.core.permissions

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE

/**
 * Current state of Android's runtime permissions granted to this application.
 */
data class RuntimePermission(
    /**
     * Whether user has granted the permission to read audio files from the device's storage.
     *
     * @see [READ_EXTERNAL_STORAGE]
     */
    val canReadAudioFiles: Boolean,
    /**
     * Whether user has granted the permission to write to or delete audio files stored on the
     * device's storage.
     *
     * This permission may never be granted on devices running Android R and newer due to being
     * superseded by Scoped Storage enforcement.
     *
     * @see [WRITE_EXTERNAL_STORAGE]
     */
    val canWriteAudioFiles: Boolean,
)
