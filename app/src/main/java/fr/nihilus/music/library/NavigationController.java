package fr.nihilus.music.library;

import android.support.annotation.IdRes;
import android.support.v4.app.FragmentManager;
import android.support.v4.media.MediaBrowserCompat;

import javax.inject.Inject;

import fr.nihilus.music.HomeActivity;
import fr.nihilus.music.R;
import fr.nihilus.music.di.ActivityScoped;
import fr.nihilus.music.ui.albums.AlbumGridFragment;
import fr.nihilus.music.ui.artists.ArtistDetailFragment;
import fr.nihilus.music.ui.artists.ArtistsFragment;
import fr.nihilus.music.ui.playlist.PlaylistsFragment;
import fr.nihilus.music.ui.songs.SongListFragment;

@ActivityScoped
public class NavigationController {
    private final FragmentManager mFm;
    private final @IdRes int mContainerId;

    @Inject
    public NavigationController(HomeActivity activity) {
        mFm = activity.getSupportFragmentManager();
        mContainerId = R.id.container;
    }

    public void navigateToAllSongs() {
        mFm.beginTransaction()
                .replace(mContainerId, new SongListFragment())
                .commitAllowingStateLoss();
    }

    public void navigateToAlbums() {
        mFm.beginTransaction()
                .replace(mContainerId, new AlbumGridFragment())
                .commitAllowingStateLoss();
    }

    public void navigateToArtists() {
        mFm.beginTransaction()
                .replace(mContainerId, new ArtistsFragment())
                .commitAllowingStateLoss();
    }

    public void navigateToArtistDetail(MediaBrowserCompat.MediaItem artist) {
        ArtistDetailFragment f = ArtistDetailFragment.newInstance(artist);
        mFm.beginTransaction()
                .replace(mContainerId, f)
                .addToBackStack(ArtistDetailFragment.BACKSTACK_ENTRY)
                .commitAllowingStateLoss();
    }

    public void navigateToPlaylists() {
        mFm.beginTransaction()
                .replace(mContainerId, new PlaylistsFragment())
                .commitAllowingStateLoss();
    }
}
