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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.nihilus.music.core.lifecycle.ApplicationLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Android runtime permissions.
 * This repository exposes a [live view][permissions] of which permissions have been granted.
 *
 * Because requesting runtime permissions is not possible without displaying dialogs in the UI,
 * this repository is unable to directly request permissions. Instead, these permissions must be
 * requested by an Activity, then [refreshed][refreshPermissions] so that observing components
 * could adjust their behavior.
 */
@Singleton
class PermissionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationLifecycle private val owner: LifecycleOwner
) {
    private val _permissions = MutableStateFlow(readPermissions())

    /**
     * Live state of runtime permissions required by the app.
     *
     * This value is updated regularly, but won't be immediately updated as soon as a permission is
     * granted. You may want to force refreshing permissions using [refreshPermissions] after user
     * has granted some permissions.
     */
    val permissions: StateFlow<RuntimePermission> = _permissions.asStateFlow()

    init {
        // Refresh permissions when app moves to the foreground, in case user grants permission
        // directly from Android settings.
        owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                refreshPermissions()
            }
        })
    }

    /**
     * Request to refresh the state of runtime permissions.
     * This may be necessary right after granting permissions.
     */
    fun refreshPermissions() {
        _permissions.value = readPermissions()
    }

    private fun readPermissions() = RuntimePermission(
        canReadAudioFiles = hasReadMediaPermission(),
        canWriteAudioFiles = isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE),
    )

    private fun hasReadMediaPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isPermissionGranted(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun isPermissionGranted(permissionName: String): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            permissionName
        ) == PackageManager.PERMISSION_GRANTED
}
