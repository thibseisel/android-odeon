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

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import fr.nihilus.music.media.permissions.RevocablePermission
import fr.nihilus.music.media.permissions.RuntimePermissions
import fr.nihilus.music.media.provider.SQLiteMediaStoreModule
import javax.inject.Named

/**
 * Provides fake implementations of [ContentResolverDelegate], [RuntimePermissions],
 * [Clock] and [FileSystem] that simulate the behavior of the Android system in tests.
 *
 * Components that include this module can optionally set the start time of the test clock
 * by binding a [Long] value [qualified by the name][Named] `TestClock.startTime`.
 */
@Module(includes = [SQLiteMediaStoreModule::class])
internal abstract class SimulatedSystemModule {

    @Binds
    abstract fun bindsTestPermissionChecker(permissions: RevocablePermission): RuntimePermissions

    @Binds
    abstract fun bindsTestClock(clock: TestClock): Clock

    @Module
    companion object {

        @JvmStatic
        @Provides @Reusable
        fun providesSimulatedFileSystem(): FileSystem = SimulatedFileSystem()

        @JvmStatic
        @Provides
        fun providesTestClock(
            @Named("TestClock.startTime") startTime: Long?
        ) = TestClock(startTime ?: 0L)
    }
}