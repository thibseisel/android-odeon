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

package fr.nihilus.music.media.usage

import fr.nihilus.music.media.di.ServiceScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Manages reading and writing media usage statistics.
 */
internal interface MediaUsageManager {

    /**
     * Report that the track with the given [trackId] has been played until the end.
     *
     * @param trackId The unique identifier of the track that has been played.
     */
    fun reportCompletion(trackId: Long)
}

/**
 * Implementation of [MediaUsageManager] that performs operations using Kotlin Coroutines,
 * bridging to the RxJava World.
 *
 * @param scope The scope coroutines should be executed into.
 * @param usageDao The DAO that controls storage of usage statistics.
 */
@ServiceScoped
internal class RxBridgeUsageManager
@Inject constructor(
    private val scope: CoroutineScope,
    private val usageDao: MediaUsageDao
) : MediaUsageManager {

    override fun reportCompletion(trackId: Long) {
        scope.launch {
            val singleEventList = listOf(MediaUsageEvent(trackId))
            usageDao.recordUsageEvents(singleEventList)
        }
    }
}