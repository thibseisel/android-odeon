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

package fr.nihilus.music.library.cleanup

import androidx.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import fr.nihilus.music.core.ui.viewmodel.ViewModelKey

@Module
abstract class CleanupModule {

    @ContributesAndroidInjector
    abstract fun cleanupActivity(): CleanupActivity

    @ContributesAndroidInjector
    abstract fun cleanupFragment(): CleanupFragment

    @Binds @IntoMap
    @ViewModelKey(CleanupViewModel::class)
    abstract fun bindsCleanupViewModel(viewModel: CleanupViewModel): ViewModel
}