/*
 * Copyright 2021 Thibault Seisel
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

package fr.nihilus.music.core.flow

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

sealed class Materialized<out T> {
    class Value<T>(val value: T) : Materialized<T>()
    class Error(val error: Throwable) : Materialized<Nothing>()
}

fun <T> Flow<T>.materialize(): Flow<Materialized<T>> = this
    .map<T, Materialized<T>> { Materialized.Value(it) }
    .catch { emit(Materialized.Error(it)) }

fun <T> Flow<Materialized<T>>.dematerialize(): Flow<T> = map {
    when (it) {
        is Materialized.Value -> it.value
        is Materialized.Error -> throw it.error
    }
}