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

package fr.nihilus.music.service

import android.content.Intent
import androidx.media.MediaBrowserServiceCompat
import dagger.android.AndroidInjection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

abstract class BaseBrowserService : MediaBrowserServiceCompat(), CoroutineScope {

    private var isStarted = false

    private lateinit var scopeJob: Job
    override val coroutineContext: CoroutineContext
        get() = scopeJob + Dispatchers.Main.immediate

    override fun onCreate() {
        super.onCreate()
        scopeJob = SupervisorJob()
        AndroidInjection.inject(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isStarted = true
        return super.onStartCommand(intent, flags, startId)
    }

    protected fun startSelf() {
        if (!isStarted) {
            startService(Intent(this, this::class.java))
        }
    }

    protected fun stop() {
        if (isStarted) {
            isStarted = false
            stopSelf()
        }
    }

    override fun onDestroy() {
        scopeJob.cancel()
        super.onDestroy()
    }
}