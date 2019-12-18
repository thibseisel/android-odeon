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

package fr.nihilus.music.core.worker

import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Provider

/**
 * Provides dependencies required for the configuration of [androidx.work.WorkManager].
 */
@Module
abstract class WorkManagerModule {

    @Binds
    internal abstract fun bindsWorkerFactory(factory: DaggerWorkerFactory): WorkerFactory

    @Module
    internal companion object {

        /**
         * Provides an empty map of factories.
         * This is temporary and is only required for compilation until the Spotify Worker is ready.
         */
        @JvmStatic @Provides
        fun providesPlaceholderFactories(): Map<Class<out ListenableWorker>, @JvmSuppressWildcards Provider<SingleWorkerFactory>> = emptyMap()
    }
}