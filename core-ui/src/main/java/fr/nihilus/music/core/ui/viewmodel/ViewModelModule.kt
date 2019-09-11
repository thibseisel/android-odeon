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

package fr.nihilus.music.core.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module

/**
 * Provides the application with a [ViewModelProvider.Factory] implementation
 * that creates [ViewModel] instances automatically from the specified [ViewModel] class.
 *
 * For a [ViewModel] subclass to be created by the factory, it should be registered like the following:
 * ```
 * abstract class MyModule {
 *
 *     @Binds @IntoMap
 *     @ViewModelKey(MyViewModel::class)
 *     abstract fun registerMyViewModel(viewModel: MyViewModel): ViewModel
 * }
 * ```
 */
@Module
abstract class ViewModelModule {

    @Binds
    internal abstract fun bindsViewModels(factory: DaggerViewModelFactory): ViewModelProvider.Factory
}