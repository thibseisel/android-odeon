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

package fr.nihilus.music.service

import dagger.Module
import dagger.android.ContributesAndroidInjector
import fr.nihilus.music.media.dagger.MediaSourceModule
import fr.nihilus.music.service.playback.PlaybackModule

/**
 * Configures [MusicService]-related bindings, providing dependency injection for the whole `media` library module.
 * This Dagger module should be installed in the root component.
 */
@Module
abstract class MusicServiceModule {

    @ServiceScoped
    @ContributesAndroidInjector(modules = [
        ServiceBindingsModule::class,
        MediaSessionModule::class,
        MediaSourceModule::class,
        PlaybackModule::class
    ])
    abstract fun musicService(): MusicService
}