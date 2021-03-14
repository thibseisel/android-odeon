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

package fr.nihilus.music.spotify.dagger

import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import fr.nihilus.music.core.worker.SingleWorkerFactory
import fr.nihilus.music.core.worker.WorkerKey
import fr.nihilus.music.spotify.SpotifySyncWorker

/**
 * Configure [SpotifySyncWorker] to be used with [WorkManager].
 *
 * This modules provides instances of the factory that should be used when [WorkManager] attempts
 * to initialize that worker so that its dependencies could be provided with Dagger.
 *
 * @see fr.nihilus.music.core.worker.DaggerWorkerFactory
 */
@Module(includes = [SpotifyManagerModule::class])
@InstallIn(SingletonComponent::class)
abstract class SpotifyWorkerModule {

    @Binds @IntoMap
    @WorkerKey(SpotifySyncWorker::class)
    internal abstract fun bindsSpotifyWorker(factory: SpotifySyncWorker.Factory): SingleWorkerFactory
}