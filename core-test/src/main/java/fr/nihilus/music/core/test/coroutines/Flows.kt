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

package fr.nihilus.music.core.test.coroutines

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * A flow that does not emit any item and never terminates.
 * This could be used to simulate a an infinite flow sequence.
 */
@UseExperimental(FlowPreview::class)
object NeverFlow : AbstractFlow<Nothing>() {
    override suspend fun collectSafely(collector: FlowCollector<Nothing>) {
        suspendCancellableCoroutine<Nothing> {}
    }
}