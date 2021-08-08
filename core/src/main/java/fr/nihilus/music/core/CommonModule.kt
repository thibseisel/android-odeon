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

package fr.nihilus.music.core

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.nihilus.music.core.os.*
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CommonModule {

    @Binds
    internal abstract fun bindsSystemPermissions(
        permissions: SystemRuntimePermissions
    ): RuntimePermissions

    @Binds
    internal abstract fun bindsSystemClock(clock: DeviceClock): Clock

    @Binds
    internal abstract fun bindsAndroidFileSystem(fileSystem: AndroidFileSystem): FileSystem

    internal companion object {

        @Provides @Singleton
        fun providesSharedPreferences(@ApplicationContext appContext: Context): SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(appContext)
    }
}