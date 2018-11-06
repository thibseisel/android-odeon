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

package fr.nihilus.music.utils

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.Observer

/**
 * Useful extensions to perform functional operations on LiveData.
 */

/**
 * Register an observer for this LiveData using a more idiomatic Kotlin syntax.
 *
 * @see LiveData.observe
 */
inline fun <T> LiveData<T>.observeK(owner: LifecycleOwner, crossinline observer: (T?) -> Unit) {
    observe(owner, Observer { observer(it) })
}

/**
 * Filters values of this LiveData,
 * notifying observers only if the new value satisfies a given [predicate].
 *
 * @param predicate A function that evaluates each new value, returning `true` if it passes the filter.
 * @return A LiveData that notifies for changes to the source LiveData.
 */
inline fun <T> LiveData<T>.filter(crossinline predicate: (T?) -> Boolean): LiveData<T> =
    MediatorLiveData<T>().also { mediator ->
        mediator.addSource(this) {
            if (predicate(it)) {
                mediator.value = it
            }
        }
    }

inline fun <T, R> LiveData<T>.map(crossinline transform: (T?) -> R?): LiveData<R> =
    MediatorLiveData<R>().also { mediator ->
        mediator.addSource(this) {
            mediator.value = transform(it)
        }
    }