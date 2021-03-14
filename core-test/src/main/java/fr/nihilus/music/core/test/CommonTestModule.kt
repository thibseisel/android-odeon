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

package fr.nihilus.music.core.test

import android.content.Context
import android.content.SharedPreferences
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.nihilus.music.core.os.Clock
import fr.nihilus.music.core.os.FileSystem
import fr.nihilus.music.core.os.RuntimePermissions
import fr.nihilus.music.core.settings.SettingsModule
import fr.nihilus.music.core.test.os.NoopFileSystem
import fr.nihilus.music.core.test.os.RevocablePermission
import fr.nihilus.music.core.test.os.TestClock
import javax.inject.Named
import javax.inject.Singleton

/**
 * Components that include this module can optionally set the start time of the test clock
 * by binding a [Long] value [qualified by the name][Named] `TestClock.startTime`.
 */
@Module(includes = [SettingsModule::class])
abstract class CommonTestModule {

    @Binds
    abstract fun bindsRevocablePermissions(permissions: RevocablePermission): RuntimePermissions

    @Binds
    abstract fun bindsTestClock(clock: TestClock): Clock

    internal companion object {

        @Provides @Singleton
        fun providesTestClock(
            @Named("TestClock.startTime") startTime: Long?
        ) = TestClock(startTime ?: 0L)

        @Provides @Singleton
        fun providesTestPermissions() = RevocablePermission()

        @Provides
        fun providesFileSystem(): FileSystem = NoopFileSystem

        @Provides @Singleton
        fun providesSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
            context.getSharedPreferences("test", Context.MODE_PRIVATE)
    }
}