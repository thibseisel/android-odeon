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

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import fr.nihilus.music.client.BrowserViewModel
import fr.nihilus.music.client.DaggerViewModelFactory
import fr.nihilus.music.client.ViewModelKey
import fr.nihilus.music.library.MusicLibraryViewModel
import fr.nihilus.music.library.albums.AlbumGridViewModel
import fr.nihilus.music.library.artists.ArtistListViewModel
import fr.nihilus.music.library.artists.detail.ArtistDetailViewModel
import fr.nihilus.music.library.nowplaying.NowPlayingViewModel
import fr.nihilus.music.library.playlists.AddToPlaylistViewModel
import fr.nihilus.music.library.playlists.MembersViewModel
import fr.nihilus.music.library.playlists.NewPlaylistViewModel
import fr.nihilus.music.library.playlists.PlaylistsViewModel
import fr.nihilus.music.library.songs.SongListViewModel

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

    @Binds @IntoMap
    @ViewModelKey(MusicLibraryViewModel::class)
    abstract fun bindsMusicLibraryViewModel(viewModel: MusicLibraryViewModel): ViewModel

    @Binds @IntoMap
    @ViewModelKey(NowPlayingViewModel::class)
    abstract fun bindsNowPlayingViewModel(viewModel: NowPlayingViewModel): ViewModel

    @Binds @IntoMap
    @ViewModelKey(SongListViewModel::class)
    abstract fun bindsSongListViewModel(viewModel: SongListViewModel): ViewModel

    @Binds @IntoMap
    @ViewModelKey(AddToPlaylistViewModel::class)
    abstract fun bindsAddToPlaylistViewModel(viewModel: AddToPlaylistViewModel): ViewModel

    @Binds @IntoMap
    @ViewModelKey(NewPlaylistViewModel::class)
    abstract fun bindsNewPlaylistViewModel(viewModel: NewPlaylistViewModel): ViewModel

    @Binds @IntoMap
    @ViewModelKey(AlbumGridViewModel::class)
    abstract fun bindsAlbumGridViewModel(viewModel: AlbumGridViewModel): ViewModel

    @Binds @IntoMap
    @ViewModelKey(ArtistListViewModel::class)
    abstract fun bindsArtistListViewModel(viewModel: ArtistListViewModel): ViewModel

    @Binds @IntoMap
    @ViewModelKey(ArtistDetailViewModel::class)
    abstract fun bindsArtistDetailViewModel(viewModel: ArtistDetailViewModel): ViewModel

    @Binds @IntoMap
    @ViewModelKey(PlaylistsViewModel::class)
    abstract fun bindsPlaylistsViewModel(viewModel: PlaylistsViewModel): ViewModel

    @Binds @IntoMap
    @ViewModelKey(MembersViewModel::class)
    abstract fun bindsMembersViewModel(viewModel: MembersViewModel): ViewModel
}
