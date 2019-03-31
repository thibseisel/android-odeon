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

package fr.nihilus.music.base

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

/**
 * A base ViewModel that define the scope of launched Kotlin coroutines
 * to the lifecycle of the ViewModel.
 * All running coroutines will be canceled when the ViewModel is cleared.
 */
abstract class BaseViewModel : ViewModel(), CoroutineScope {
    private val lifetime = SupervisorJob()
    override val coroutineContext: CoroutineContext = lifetime + Dispatchers.Main.immediate

    override fun onCleared() {
        lifetime.cancel()
        super.onCleared()
    }
}