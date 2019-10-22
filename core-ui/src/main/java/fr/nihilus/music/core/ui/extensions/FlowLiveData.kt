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

package fr.nihilus.music.core.ui.extensions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Collect each value emitted by a source [Flow] to a [LiveData].
 * @receiver The source flow.
 * @param scope The scope in which the flow will be collected.
 * @return A LiveData whose value is determined from the latest emitted value of the source flow.
 */
fun <T> Flow<T>.consumeAsLiveData(scope: CoroutineScope): LiveData<T> {
    val liveData = MutableLiveData<T>()
    onEach { liveData.value = it }.launchIn(scope)
    return liveData
}