/*
 * Copyright 2020 Thibault Seisel
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

package fr.nihilus.music.spotify.dagger

import dagger.Binds
import dagger.Module
import fr.nihilus.music.spotify.manager.SpotifyManager
import fr.nihilus.music.spotify.manager.SpotifyManagerImpl

/**
 * Provides the implementation of [SpotifyManager].
 */
@Module
abstract class SpotifyManagerModule {

    @Binds
    internal abstract fun bindsManagerImpl(impl: SpotifyManagerImpl): SpotifyManager
}