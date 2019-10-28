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
import fr.nihilus.music.media.provider.*
import fr.nihilus.music.media.repo.MediaRepository
import fr.nihilus.music.media.repo.MediaRepositoryImpl
import fr.nihilus.music.media.usage.UsageManager
import fr.nihilus.music.media.usage.UsageManagerImpl

/**
 * Define relations in the object graph for the "media" group of features.
 */
@Module(includes = [
    MediaStoreModule::class
])
abstract class MediaSourceModule {

    @Binds
    internal abstract fun bindsUsageManager(impl: UsageManagerImpl): UsageManager

    @Binds
    internal abstract fun bindsMediaRepository(impl: MediaRepositoryImpl): MediaRepository

    @Binds
    internal abstract fun bindsMediaDao(impl: MediaDaoImpl): MediaDao

    @Binds
    internal abstract fun bindsMediaProvider(mediaStoreProvider: MediaStoreProvider): MediaProvider
}
