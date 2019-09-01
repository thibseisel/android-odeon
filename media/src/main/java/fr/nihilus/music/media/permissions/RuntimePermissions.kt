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

package fr.nihilus.music.media.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.Reusable
import javax.inject.Inject

/**
 * An utility that checks the current application's dangerous permissions at runtime.
 */
interface RuntimePermissions {
    /** Whether files stored on the device's external storage can be read by this application. */
    val canReadExternalStorage: Boolean
    /** Whether this application can write to files stored on the external storage. */
    val canWriteToExternalStorage: Boolean
}

/**
 * Real implementation of the [RuntimePermissions].
 * This checks permission using the provided [application context][context].
 */
@Reusable
class SystemRuntimePermissions
@Inject constructor(
    private val context: Context
) : RuntimePermissions {

    override val canReadExternalStorage: Boolean
        get() = isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
    override val canWriteToExternalStorage: Boolean
        get() = isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private fun isPermissionGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}