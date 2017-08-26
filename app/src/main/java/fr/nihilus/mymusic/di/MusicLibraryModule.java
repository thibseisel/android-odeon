package fr.nihilus.mymusic.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fr.nihilus.mymusic.ui.albums.AlbumGridFragment;
import fr.nihilus.mymusic.ui.artists.ArtistDetailFragment;
import fr.nihilus.mymusic.ui.artists.ArtistsFragment;
import fr.nihilus.mymusic.ui.playlist.NewPlaylistFragment;
import fr.nihilus.mymusic.ui.playlist.PlaylistsFragment;
import fr.nihilus.mymusic.ui.songs.SongListFragment;

@Module
abstract class MusicLibraryModule {

    @FragmentScoped
    @ContributesAndroidInjector
    abstract SongListFragment contributeSongListFragment();

    @FragmentScoped
    @ContributesAndroidInjector
    abstract AlbumGridFragment contributeAlbumGridFragment();

    @FragmentScoped
    @ContributesAndroidInjector
    abstract ArtistsFragment contributeArtistsFragment();

    @FragmentScoped
    @ContributesAndroidInjector
    abstract ArtistDetailFragment contributeArtistDetailFragment();

    @FragmentScoped
    @ContributesAndroidInjector
    abstract PlaylistsFragment contributePlaylistsFragment();

    @FragmentScoped
    @ContributesAndroidInjector
    abstract NewPlaylistFragment contributeNewPlaylistFragment();
}
