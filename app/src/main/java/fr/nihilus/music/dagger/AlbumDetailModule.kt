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

package fr.nihilus.music.dagger

import androidx.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import fr.nihilus.music.client.ViewModelKey
import fr.nihilus.music.library.albums.AlbumDetailActivity
import fr.nihilus.music.library.albums.AlbumDetailFragment
import fr.nihilus.music.library.albums.AlbumDetailViewModel

@Module
internal abstract class AlbumDetailModule {

    @ContributesAndroidInjector
    abstract fun albumDetailActivity(): AlbumDetailActivity

    @ContributesAndroidInjector
    abstract fun albumDetailFragment(): AlbumDetailFragment

    @Binds @IntoMap
    @ViewModelKey(AlbumDetailViewModel::class)
    abstract fun bindAlbumDetailViewModel(viewModel: AlbumDetailViewModel): ViewModel
}