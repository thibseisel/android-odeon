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
abstract class FragmentsBuilderModule {

    @ContributesAndroidInjector
    abstract SongListFragment contributeSongListFragment();

    @ContributesAndroidInjector
    abstract AlbumGridFragment contributeAlbumGridFragment();

    @ContributesAndroidInjector
    abstract ArtistsFragment contributeArtistsFragment();

    @ContributesAndroidInjector
    abstract ArtistDetailFragment contributeArtistDetailFragment();

    @ContributesAndroidInjector
    abstract PlaylistsFragment contributePlaylistsFragment();

    @ContributesAndroidInjector
    abstract NewPlaylistFragment contributeNewPlaylistFragment();
}
