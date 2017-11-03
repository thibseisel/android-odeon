package fr.nihilus.music.library

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.support.v4.media.MediaBrowserCompat
import fr.nihilus.music.Constants
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
private const val KEY_FIRST_TAG = "first_tag"

/**
 * Manages the navigation between screens in [HomeActivity].
 *
 * Clients are required to save and restore this navigation controller's state
 * on configuration changes with the [saveState] and [restoreState].
 */
@ActivityScoped
class NavigationController
@Inject constructor(activity: HomeActivity) {

    private val mFm: FragmentManager = activity.supportFragmentManager
    private val mContainerId: Int = R.id.container
    private var mFirstTag: String? = null

    /**
     * An event that notifies when the currently shown fragment changes.
     *
     * The provided function has an argument that is the id of the fragment.
     */
    var routeChangeListener: (Int) -> Unit = {}

    init {
        mFm.addOnBackStackChangedListener {
            // Listen for back stack changes and emits an event with the id of the displayed fragment
            mFm.findFragmentById(mContainerId)?.let { fragment ->
                val fragmentId = fragment.arguments!!.getInt(Constants.FRAGMENT_ID)
                routeChangeListener(fragmentId)
            }
        }
    }

    /**
     * Shows the home screen.
     */
    fun navigateToHome() {
        val fragment = findOrCreateFragment(MediaID.ID_AUTO, HomeFragment.Factory::newInstance)
        showFragment(MediaID.ID_AUTO, fragment)
    }

    /**
     * Shows the list of all tracks available.
     */
    fun navigateToAllSongs() {
        val fragment = findOrCreateFragment(MediaID.ID_MUSIC, SongListFragment::newInstance)
        showFragment(MediaID.ID_MUSIC, fragment)
    }

    /**
     * Shows the list of all albums.
     */
    fun navigateToAlbums() {
        val fragment = findOrCreateFragment(MediaID.ID_ALBUMS, AlbumGridFragment::newInstance)
        showFragment(MediaID.ID_ALBUMS, fragment)
    }

    /**
     * Shows the list of all artists.
     */
    fun navigateToArtists() {
        val fragment = findOrCreateFragment(MediaID.ID_ARTISTS, ArtistsFragment::newInstance)
        showFragment(MediaID.ID_ARTISTS, fragment)
    }

    /**
     * Shows the detail of a specific artist.
     * This includes its produced albums and tracks.
     *
     * @param artist The artist from whose details are to be displayed
     */
    fun navigateToArtistDetail(artist: MediaBrowserCompat.MediaItem) {
        val tag = artist.mediaId ?: throw IllegalArgumentException("Artist should have a mediaId")
        val fragment = findOrCreateFragment(tag) {
            ArtistDetailFragment.newInstance(artist)
        }

        showFragment(tag, fragment)

    }

    /**
     * Shows the list of user defined playlists.
     */
    fun navigateToPlaylists() {
        val fragment = findOrCreateFragment(MediaID.ID_PLAYLISTS, PlaylistsFragment::newInstance)
        showFragment(MediaID.ID_PLAYLISTS, fragment)
    }

    /**
     * Shows the list of tracks that are part of a specific playlist.
     *
     * @param playlist the playlist from whose tracks are to be displayed
     */
    fun navigateToPlaylistDetails(playlist: MediaBrowserCompat.MediaItem) {
        val tag = playlist.mediaId ?: throw IllegalArgumentException("Playlist should have a mediaId")
        val fragment = findOrCreateFragment(tag) {
            // Only user-defined playlists should be deletable
            val rootId = MediaID.getHierarchy(tag)[0]
            MembersFragment.newInstance(playlist, deletable = MediaID.ID_PLAYLISTS == rootId)
        }

        showFragment(tag, fragment)
    }

    /**
     * Manually go back to the previous screen, if any.
     *
     * This is the same as pressing the back button except that the application won't be closed.
     */
    fun navigateBack() = mFm.popBackStack()

    /**
     * Shows the view that correspond to a specific media id.
     *
     * @param mediaId The media id that represents the screen to display
     */
    fun navigateToMediaId(mediaId: String) {
        val root = MediaID.getHierarchy(mediaId)[0]
        when (root) {
            MediaID.ID_AUTO -> navigateToHome()
            MediaID.ID_MUSIC -> navigateToAllSongs()
            MediaID.ID_ALBUMS -> navigateToAlbums()
            MediaID.ID_ARTISTS -> navigateToArtists()
            MediaID.ID_PLAYLISTS -> navigateToPlaylists()
            else -> throw UnsupportedOperationException("Unsupported media ID: $mediaId")
        }
    }

    /**
     * Saves this navigation controller's state into the activity state.
     * This is necessary to ensure that the first displayed screen is properly restored after
     * a configuration change.
     *
     * @param outState The bundle from [android.app.Activity.onSaveInstanceState]
     */
    fun saveState(outState: Bundle?) {
        outState?.putString(KEY_FIRST_TAG, mFirstTag)
    }

    /**
     * Restores the state of this navigation controller from the specified bundle.
     *
     * @param savedInstanceState The bundle from [android.app.Activity.onRestoreInstanceState]
     */
    fun restoreState(savedInstanceState: Bundle?) {
        mFirstTag = savedInstanceState?.getString(KEY_FIRST_TAG)
    }

    /**
     * Retrieve a fragment with the specified tag from the fragment manager,
     * or create it if it does not exist.
     * @param tag the tag of the fragment to retrieve from the fragment manager
     * @param provider a function that provides the requested fragment if does not exist
     * @return the retrieved fragment, or the provided one if not in fragment manager
     */
    private inline fun findOrCreateFragment(tag: String, provider: () -> Fragment) =
            mFm.findFragmentByTag(tag) ?: provider()

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
                    mFm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
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

            val fragmentId = fragment.arguments!!.getInt(Constants.FRAGMENT_ID)
            routeChangeListener(fragmentId)
        }
    }
}
