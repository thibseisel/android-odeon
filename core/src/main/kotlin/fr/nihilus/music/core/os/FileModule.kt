/*
 * Copyright 2021 Thibault Seisel
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

package fr.nihilus.music.core.os

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Qualifier

/**
 * Provides files and folders.
 * Definitions in this module may be replaced for testing purposes.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object FileModule {
    private const val PLAYLIST_ICONS_FOLDER = "playlist_icons"
    private const val PROVIDER_ICONS_PATH = "icons"

    @Provides @PlaylistIconDir
    fun providesPlaylistIconDir(@ApplicationContext appContext: Context): File {
        val internalStorageRoot = appContext.filesDir
        return File(internalStorageRoot, PLAYLIST_ICONS_FOLDER)
    }

    @Provides @IconContentUri
    fun providesBaseIconUri(@ApplicationContext appContext: Context): Uri {
        val applicationId = appContext.packageName
        val providerAuthority = "$applicationId.provider"
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(providerAuthority)
            .appendPath(PROVIDER_ICONS_PATH)
            .build()
    }
}

/**
 * Qualifier for a [file directory][File] where generated playlist icons are stored
 * on the device's storage.
 * That directory is not guaranteed to exist.
 */
@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class PlaylistIconDir

@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class IconContentUri