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

package fr.nihilus.music.spotify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import fr.nihilus.music.core.worker.SingleWorkerFactory
import fr.nihilus.music.spotify.manager.SpotifyManager
import javax.inject.Inject
import javax.inject.Provider

/**
 * A tasks for deferring download of media metadata from the Spotify API.
 * The execution of this task requires an internet connection.
 */
class SpotifySyncWorker(
    context: Context,
    params: WorkerParameters,
    private val manager: SpotifyManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        manager.sync()
        return Result.success()
    }

    internal class Factory @Inject constructor(
        private val manager: Provider<SpotifyManager>
    ) : SingleWorkerFactory {

        override fun createWorker(appContext: Context, params: WorkerParameters): ListenableWorker {
            return SpotifySyncWorker(appContext, params, manager.get())
        }
    }
}