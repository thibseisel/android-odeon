package fr.nihilus.mymusic.library;

import android.support.annotation.IdRes;
import android.support.v4.app.FragmentManager;
import android.support.v4.media.MediaBrowserCompat;

import javax.inject.Inject;

import fr.nihilus.mymusic.HomeActivity;
import fr.nihilus.mymusic.R;
import fr.nihilus.mymusic.di.ActivityScoped;
import fr.nihilus.mymusic.ui.albums.AlbumGridFragment;
import fr.nihilus.mymusic.ui.artists.ArtistDetailFragment;
import fr.nihilus.mymusic.ui.artists.ArtistsFragment;
import fr.nihilus.mymusic.ui.playlist.PlaylistsFragment;
import fr.nihilus.mymusic.ui.songs.SongListFragment;

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
