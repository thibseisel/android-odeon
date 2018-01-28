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

package fr.nihilus.music.media

import dagger.Binds
import dagger.Module
import fr.nihilus.music.media.builtin.BuiltinModule
import fr.nihilus.music.media.cache.LruMemoryCache
import fr.nihilus.music.media.cache.MusicCache
import fr.nihilus.music.media.repo.CachedMusicRepository
import fr.nihilus.music.media.repo.MusicRepository
import fr.nihilus.music.media.source.MediaStoreMusicDao
import fr.nihilus.music.media.source.MusicDao

/**
 * Define relations in the object graph for the "media" group of features.
 * Those classes are bound to the application context and are instantiated only once.
 *
 * Binds annotated methods are used by Dagger to know which implementation
 * should be injected when asking for an abstract type.
 */
@Suppress("unused")
@Module(includes = [(BuiltinModule::class)])
internal abstract class MediaModule {

    @Binds
    abstract fun bindsMusicCache(cacheImpl: LruMemoryCache): MusicCache

    @Binds
    abstract fun bindsMusicDao(daoImpl: MediaStoreMusicDao): MusicDao

    @Binds
    abstract fun bindsMusicRepository(repoImpl: CachedMusicRepository): MusicRepository
}
