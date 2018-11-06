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

package fr.nihilus.music.di

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import fr.nihilus.music.client.BrowserViewModel
import fr.nihilus.music.client.DaggerViewModelFactory
import fr.nihilus.music.client.ViewModelKey

/**
 * Every ViewModel subclass that can be created with [DaggerViewModelFactory]
 * must be registered in this module via a Map MultiBinding.
 *
 * The key must be the actual subclass of ViewModel.
 */
@Suppress("unused")
@Module
abstract class ViewModelModule {

    @Binds
    abstract fun bindsViewModelFactory(factory: DaggerViewModelFactory): ViewModelProvider.Factory

    @Binds @IntoMap
    @ViewModelKey(BrowserViewModel::class)
    abstract fun bindsBrowserViewModel(viewModel: BrowserViewModel): ViewModel
}
