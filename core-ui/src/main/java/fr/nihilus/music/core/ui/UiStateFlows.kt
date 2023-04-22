/*
 * Copyright 2022 Thibault Seisel
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

package fr.nihilus.music.core.ui

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/**
 * Delay after which upstream flow should be cancelled.
 * This value is large enough to avoid re-subscribing after a configuration change.
 */
private const val STOP_DELAY = 5000L

/**
 * Produces a [StateFlow] that's suitable for exposing state to the UI.
 * This should primarily be used by [ViewModel]s.
 *
 * @param scope Coroutine scope in which the upstream flow is collected.
 * This typically is the [ViewModel's scope][viewModelScope].
 */
fun <T> Flow<T>.uiStateIn(scope: CoroutineScope, initialState: T): StateFlow<T> = stateIn(
    scope,
    started = SharingStarted.WhileSubscribed(STOP_DELAY),
    initialState
)

/**
 * Helper function that collects a [StateFlow] the same way we observe [LiveData].
 */
fun <T> StateFlow<T>.observe(owner: LifecycleOwner, observer: (value: T) -> Unit) {
    flowWithLifecycle(owner.lifecycle)
        .onEach(observer)
        .launchIn(owner.lifecycleScope)
}
