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

package fr.nihilus.music.ui

import android.Manifest
import android.app.Activity
import androidx.core.app.ActivityCompat

/**
 * Identifies the request to grant permission to read/write the device's external storage.
 */
const val EXTERNAL_STORAGE_REQUEST = 99

/**
 * Issue a request to grant permission to read/write the device's external storage.
 * The result will be dispatched to [Activity.onRequestPermissionsResult]
 * with the request code [EXTERNAL_STORAGE_REQUEST].
 */
fun Activity.requestExternalStoragePermission() {
    val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    ActivityCompat.requestPermissions(this, permissions, EXTERNAL_STORAGE_REQUEST)
}