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

package fr.nihilus.music.service

import dagger.Module
import dagger.android.ContributesAndroidInjector
import fr.nihilus.music.common.ExecutionContextModule
import fr.nihilus.music.database.SQLiteDatabaseModule
import fr.nihilus.music.media.actions.CustomActionModule
import fr.nihilus.music.media.di.MediaSourceModule
import fr.nihilus.music.media.di.ServiceScoped
import fr.nihilus.music.media.os.AndroidSystemModule
import fr.nihilus.music.service.playback.PlaybackModule

/**
 * Configures [MusicService]-related bindings, providing dependency injection for the whole `media` library module.
 * This Dagger module should be installed in the root component.
 */
@Module
abstract class MediaServiceModule {

    @ServiceScoped
    @ContributesAndroidInjector(modules = [
        ExecutionContextModule::class,
        ServiceBindingsModule::class,
        SQLiteDatabaseModule::class,
        AndroidSystemModule::class,
        MediaSessionModule::class,
        MediaSourceModule::class,
        PlaybackModule::class,
        CustomActionModule::class
    ])
    abstract fun musicService(): MusicService
}