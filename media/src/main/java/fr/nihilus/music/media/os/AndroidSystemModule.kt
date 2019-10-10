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

package fr.nihilus.music.media.os

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import fr.nihilus.music.media.provider.MediaStoreModule
import java.io.File
import javax.inject.Named

/**
 * Provides implementations of [ContentResolverDelegate], [FileSystem] and [Clock]
 * that delegates to the Android system.
 */
@Module(includes = [MediaStoreModule::class])
abstract class AndroidSystemModule {

    @Binds
    internal abstract fun bindsAndroidFileSystem(fileSystem: AndroidFileSystem): FileSystem

    @Module
    companion object {

        @JvmStatic
        @Provides @Named("internal")
        fun providesInternalStorageRoot(context: Context): File = context.filesDir
    }
}