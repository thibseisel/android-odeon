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

package fr.nihilus.music.library

import android.arch.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import fr.nihilus.music.HomeActivity
import fr.nihilus.music.client.ViewModelKey
import fr.nihilus.music.dagger.AlbumDetailModule
import fr.nihilus.music.library.albums.AlbumsModule
import fr.nihilus.music.library.artists.ArtistsModule
import fr.nihilus.music.library.nowplaying.NowPlayingModule
import fr.nihilus.music.library.playlists.PlaylistsModule
import fr.nihilus.music.library.songs.SongsModule
import javax.inject.Scope

/**
 * Define the scope of the client-side music library browser.
 * Dependencies annotated with this scope are alive
 * as long as the user is browsing the music library.
 */
@Scope
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class MusicLibraryScope

/**
 * Configure dependencies for the music library browsing feature.
 */
@Module(includes = [AlbumDetailModule::class])
abstract class MusicLibraryModule {

    @MusicLibraryScope
    @ContributesAndroidInjector(modules = [
        NowPlayingModule::class,
        SongsModule::class,
        AlbumsModule::class,
        ArtistsModule::class,
        PlaylistsModule::class
    ])
    abstract fun homeActivity(): HomeActivity

    @Binds @IntoMap
    @ViewModelKey(MusicLibraryViewModel::class)
    abstract fun bindsMusicLibraryViewModel(viewModel: MusicLibraryViewModel): ViewModel
}
