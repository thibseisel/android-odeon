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

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import javax.inject.Inject
import javax.inject.Provider

/**
 * A custom [WorkerFactory] that can create instances of [ListenableWorker] with Dagger.
 * This implementation uses [sub-factories][SingleWorkerFactory] to assist injection
 * of the application context and the worker parameters that are only available at runtime.
 *
 * Any attempt to create an instance of a [ListenableWorker] that is not registered by this factory
 * will fail with an exception.
 *
 * @param factories A map that associate the worker class to create to that worker factory.
 */
internal class DaggerWorkerFactory @Inject constructor(
    private val factories: Map<Class<out ListenableWorker>, @JvmSuppressWildcards Provider<SingleWorkerFactory>>
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        val workerClass = Class.forName(workerClassName)
        val factoryProvider = factories.entries.find { workerClass.isAssignableFrom(it.key) }?.value
        return factoryProvider?.get()
            ?.createWorker(appContext, workerParameters)
            ?: error("Unable to instantiate worker of type $workerClassName because no factory is available.")
    }
}