/*
 * Copyright 2018 Thibault Seisel
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

package fr.nihilus.music.media.di

import dagger.Binds
import dagger.Module
import fr.nihilus.music.media.database.DatabaseModule
import fr.nihilus.music.media.provider.MediaProvider
import fr.nihilus.music.media.provider.MediaStoreProvider
import fr.nihilus.music.media.provider.RxMediaDao
import fr.nihilus.music.media.provider.RxMediaDaoImpl
import fr.nihilus.music.media.repo.MediaRepository
import fr.nihilus.music.media.repo.MediaRepositoryImpl
import fr.nihilus.music.media.tree.BrowserTree
import fr.nihilus.music.media.tree.BrowserTreeImpl
import fr.nihilus.music.media.usage.MediaUsageManager
import fr.nihilus.music.media.usage.UsageManagerImpl

/**
 * Define relations in the object graph for the "media" group of features.
 */
@Module(includes = [DatabaseModule::class])
internal abstract class MediaSourceModule {

    @Binds
    abstract fun bindsBrowserTree(impl: BrowserTreeImpl): BrowserTree

    @Binds
    abstract fun bindsUsageManager(impl: UsageManagerImpl): MediaUsageManager

    @Binds
    abstract fun bindsMediaRepository(impl: MediaRepositoryImpl): MediaRepository

    @Binds
    abstract fun bindsRxMediaDao(impl: RxMediaDaoImpl): RxMediaDao

    @Binds
    abstract fun bindsMediaProvider(mediaStoreProvider: MediaStoreProvider): MediaProvider
}
