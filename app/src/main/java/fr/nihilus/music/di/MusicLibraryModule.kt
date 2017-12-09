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

package fr.nihilus.music.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import fr.nihilus.music.ui.HomeFragment
import fr.nihilus.music.ui.albums.AlbumGridFragment
import fr.nihilus.music.ui.artists.ArtistDetailFragment
import fr.nihilus.music.ui.artists.ArtistsFragment
import fr.nihilus.music.ui.playlist.MembersFragment
import fr.nihilus.music.ui.playlist.NewPlaylistFragment
import fr.nihilus.music.ui.playlist.PlaylistsFragment
import fr.nihilus.music.ui.songs.SongListFragment

/**
 * Enable dependency injection for Fragments attached to the main activity.
 * Every fragment defines its own scope by creating a subcomponent.
 */
@Suppress("unused")
@Module
abstract class MusicLibraryModule {

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun contributeSongListFragment(): SongListFragment

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun contributeAlbumGridFragment(): AlbumGridFragment

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun contributeArtistsFragment(): ArtistsFragment

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun contributeArtistDetailFragment(): ArtistDetailFragment

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun contributePlaylistsFragment(): PlaylistsFragment

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun contributeNewPlaylistFragment(): NewPlaylistFragment

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun contributeMembersFragment(): MembersFragment

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun contributeHomeFragment(): HomeFragment
}
