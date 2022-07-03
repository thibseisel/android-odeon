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

package fr.nihilus.music.media.dagger

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.nihilus.music.media.provider.DelegatingMediaDao
import fr.nihilus.music.media.provider.MediaDao
import fr.nihilus.music.media.usage.UsageManager
import fr.nihilus.music.media.usage.UsageManagerImpl

@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused")
abstract class MediaSourceModule {

    @Binds
    internal abstract fun bindsMediaDao(impl: DelegatingMediaDao): MediaDao

    @Binds
    internal abstract fun bindsUsageManager(impl: UsageManagerImpl): UsageManager
}
