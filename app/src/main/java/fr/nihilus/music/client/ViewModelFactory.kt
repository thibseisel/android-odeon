/*
 * Copyright 2017 Thibault Seisel
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

package fr.nihilus.music.client

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider

import fr.nihilus.music.di.ViewModelModule

import javax.inject.Inject
import javax.inject.Provider

/**
 * Creates instances of ViewModel subclasses that are registered in [ViewModelModule].
 * This enables dependency injection into the created ViewModels.
 */
class ViewModelFactory
@Inject internal constructor(
    private val creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val creator = creators[modelClass]
                ?: throw IllegalArgumentException("Unknown model class: $modelClass")
        return creator.get() as T
    }
}