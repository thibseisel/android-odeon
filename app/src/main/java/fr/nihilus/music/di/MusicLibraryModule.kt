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
