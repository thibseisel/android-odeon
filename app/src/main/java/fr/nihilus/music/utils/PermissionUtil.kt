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

package fr.nihilus.music.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity

/**
 * Groups utility functions relative to runtime permission checking.
 */
object PermissionUtil {

    /**
     * Identifies the request to grant permission to read/write the device's external storage.
     */
    const val EXTERNAL_STORAGE_REQUEST = 99

    /**
     * Checks whether user has granted permission to this application to read/write
     * the device's external storage.
     *
     * @param ctx Context of this application
     * @return `true` if permission is granted
     */
    fun hasExternalStoragePermission(ctx: Context) =
            ContextCompat.checkSelfPermission(ctx,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    /**
     * Issue a request to grant permission to read/write the device's external storage.
     * The result will be dispatched to [AppCompatActivity.onRequestPermissionsResult]
     * with the request code [EXTERNAL_STORAGE_REQUEST].
     */
    fun requestExternalStoragePermission(activity: AppCompatActivity) {
        val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ActivityCompat.requestPermissions(activity, permissions, EXTERNAL_STORAGE_REQUEST)
    }
}
