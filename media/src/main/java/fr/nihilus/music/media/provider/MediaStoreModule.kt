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

package fr.nihilus.music.media.provider

import android.content.ContentResolver
import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.nihilus.music.media.os.MediaStoreDatabase
import fr.nihilus.music.media.os.PlatformMediaStore

/**
 * Provides a [MediaStoreDatabase] that delegates calls to the system's [ContentResolver],
 * retrieving media definitions from Android's [MediaStore][android.provider.MediaStore].
 *
 * Note: this module is exceptionally visible by other modules in order to inject [MediaDao],
 * as a workaround until a suitable replacement for MediaRepository is found.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MediaStoreModule {

    @Binds
    internal abstract fun bindsMediaStoreDelegate(delegate: PlatformMediaStore): MediaStoreDatabase

    @Binds
    internal abstract fun bindsMediaStoreDao(impl: MediaStoreDao): MediaDao

    internal companion object {

        @Provides
        fun providesContentResolver(
            @ApplicationContext context: Context
        ): ContentResolver = context.contentResolver
    }
}