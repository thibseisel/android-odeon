/*
 * Copyright 2020 Thibault Seisel
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

package fr.nihilus.music.devmenu

import androidx.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import fr.nihilus.music.core.ui.dagger.PerActivity
import fr.nihilus.music.core.ui.viewmodel.ViewModelKey
import fr.nihilus.music.devmenu.features.ComposerViewModel
import fr.nihilus.music.devmenu.features.MixComposerFragment
import fr.nihilus.music.devmenu.sync.SyncFragment
import fr.nihilus.music.devmenu.sync.SyncViewModel

@Module
abstract class DevMenuModule {

    @PerActivity
    @ContributesAndroidInjector
    internal abstract fun debugActivity(): SpotifyDebugActivity

    @ContributesAndroidInjector
    internal abstract fun syncFragment(): SyncFragment

    @ContributesAndroidInjector
    internal abstract fun composerFragment(): MixComposerFragment

    @Binds @IntoMap
    @ViewModelKey(SyncViewModel::class)
    internal abstract fun bindsSyncViewModel(vm: SyncViewModel): ViewModel

    @Binds @IntoMap
    @ViewModelKey(ComposerViewModel::class)
    internal abstract fun bindsComposerViewModel(vm: ComposerViewModel): ViewModel
}