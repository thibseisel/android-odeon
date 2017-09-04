package fr.nihilus.music.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fr.nihilus.music.ui.albums.AlbumGridFragment;
import fr.nihilus.music.ui.artists.ArtistDetailFragment;
import fr.nihilus.music.ui.artists.ArtistsFragment;
import fr.nihilus.music.ui.playlist.NewPlaylistFragment;
import fr.nihilus.music.ui.playlist.PlaylistsFragment;
import fr.nihilus.music.ui.songs.SongListFragment;

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
