package fr.nihilus.music.library

import android.support.annotation.IdRes
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.support.v4.media.MediaBrowserCompat
import fr.nihilus.music.HomeActivity
import fr.nihilus.music.R
import fr.nihilus.music.di.ActivityScoped
import fr.nihilus.music.ui.HomeFragment
import fr.nihilus.music.ui.albums.AlbumGridFragment
import fr.nihilus.music.ui.artists.ArtistDetailFragment
import fr.nihilus.music.ui.artists.ArtistsFragment
import fr.nihilus.music.ui.playlist.MembersFragment
import fr.nihilus.music.ui.playlist.PlaylistsFragment
import fr.nihilus.music.ui.songs.SongListFragment
import fr.nihilus.music.utils.MediaID
import javax.inject.Inject

private const val TAG = "NavigationControl"

@ActivityScoped
class NavigationController
@Inject constructor(activity: HomeActivity) {

    private val mFm: FragmentManager = activity.supportFragmentManager
    @IdRes private val mContainerId: Int = R.id.container
    private lateinit var mFirstTag: String

    fun navigateToHome() {
        val fragment = findOrCreateFragment(MediaID.ID_AUTO, ::HomeFragment)
        showFragment(MediaID.ID_AUTO, fragment)
    }

    fun navigateToAllSongs() {
        val fragment = findOrCreateFragment(MediaID.ID_MUSIC, ::SongListFragment)
        showFragment(MediaID.ID_MUSIC, fragment)
    }

    fun navigateToAlbums() {
        val fragment = findOrCreateFragment(MediaID.ID_ALBUMS, ::AlbumGridFragment)
        showFragment(MediaID.ID_ALBUMS, fragment)
    }

    fun navigateToArtists() {
        val fragment = findOrCreateFragment(MediaID.ID_ARTISTS, ::ArtistsFragment)
        showFragment(MediaID.ID_ARTISTS, fragment)
    }

    fun navigateToArtistDetail(artist: MediaBrowserCompat.MediaItem) {
        val tag = artist.mediaId!!
        val fragment = findOrCreateFragment(tag) {
            ArtistDetailFragment.newInstance(artist)
        }

        showFragment(tag, fragment)

    }

    fun navigateToPlaylists() {
        val fragment = findOrCreateFragment(MediaID.ID_PLAYLISTS, ::PlaylistsFragment)
        showFragment(MediaID.ID_PLAYLISTS, fragment)
    }

    fun navigateToPlaylistDetails(playlist: MediaBrowserCompat.MediaItem) {
        val tag = playlist.mediaId!!
        val fragment = findOrCreateFragment(tag) {
            MembersFragment.newInstance(playlist)
        }

        showFragment(tag, fragment)
    }

    fun navigateBack() {
        mFm.popBackStack()
    }

    /**
     * Retrieve a fragment with the specified tag from the fragment manager,
     * or create it if it does not exist.
     * @param tag the tag of the fragment to retrieve from the fragment manager
     * @param provider a function that provides the requested fragment if does not exist
     * @return the retrieved fragment, or the provided one if not in fragment manager
     */
    private inline fun findOrCreateFragment(tag: String, provider: () -> Fragment) =
            mFm.findFragmentByTag(tag) ?: provider.invoke()

    /**
     * Display a given fragment in the main container view.
     * Subsequent fragments to show will be added to a transaction,
     * so that the user can go back to the previously shown fragment when pushing back button.
     *
     * If the fragment to show has already been displayed at a given moment, it will be shown again,
     * and all fragment displayed after that moment will be forgotten.
     *
     * Example :
     * ```
     * Action | Stack
     * ---------------
     * Show A | A
     * Show B | B, A
     * Back   | A
     * Show C | C, A
     * Show A | A
     * ```
     *
     * @param tag the tag that will be associated to the fragment to show.
     * It will be used when checking if fragment has already been shown.
     * @param fragment the fragment to show
     */
    private fun showFragment(tag: String, fragment: Fragment) {
        if (mFm.findFragmentById(mContainerId) != null) {
            // Is the fragment to show already on the back stack ?
            if (mFm.findFragmentByTag(tag) != null) {
                if (tag == mFirstTag) {
                    // Pop all transactions if the fragment to show is the first one added
                    val firstEntry = mFm.getBackStackEntryAt(0)
                    mFm.popBackStack(firstEntry.name, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                } else {
                    // Pop until showing the fragment
                    mFm.popBackStack(tag, 0)
                }
            } else {
                mFm.beginTransaction()
                        .replace(mContainerId, fragment, tag)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .addToBackStack(tag)
                        .commit()
            }
        } else {
            // This is the first fragment to be shown. Just add it.
            mFirstTag = tag
            mFm.beginTransaction()
                    .add(mContainerId, fragment, tag)
                    .commit()
        }
    }
}
