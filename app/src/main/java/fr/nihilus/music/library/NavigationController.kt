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

import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import fr.nihilus.music.HomeActivity
import fr.nihilus.music.RouteChangeListener
import fr.nihilus.music.library.albums.AlbumGridFragment
import fr.nihilus.music.library.artists.ArtistListFragment
import fr.nihilus.music.library.artists.detail.ArtistDetailFragment
import fr.nihilus.music.library.playlists.MembersFragment
import fr.nihilus.music.library.playlists.PlaylistsFragment
import fr.nihilus.music.library.songs.SongListFragment
import fr.nihilus.music.media.*
import fr.nihilus.music.settings.SettingsActivity
import javax.inject.Inject

/**
 * A argument key referring to the identifier of a fragment.
 *
 * Multiple fragments may share the same id: in this case they should be considered the same,
 * for example they should belong to the same navigation hierarchy.
 *
 * Type: `Int`
 */
const val FRAGMENT_ID = "fragment_id"
const val REQUEST_SETTINGS = 42

private const val KEY_FIRST_TAG = "first_tag"

/**
 * Manages the navigation between screens in [HomeActivity].
 *
 * Clients are required to save and restore this navigation controller's state
 * on configuration changes with the [saveState] and [restoreState].
 */
@MusicLibraryScope
class NavigationController
@Inject constructor(
    private val activity: HomeActivity
) {

    private val fm: androidx.fragment.app.FragmentManager = activity.supportFragmentManager
    private val containerId: Int = R.id.container
    private var firstTag: String? = null

    /**
     * An event that notifies when the currently shown fragment changes.
     *
     * The provided function has an argument that is the id of the fragment.
     */
    var routeChangeListener: RouteChangeListener = {}

    init {
        fm.addOnBackStackChangedListener {
            // Listen for back stack changes and emits an event with the id of the displayed fragment
            fm.findFragmentById(containerId)?.let { fragment ->
                val fragmentId = fragment.arguments!!.getInt(FRAGMENT_ID)
                routeChangeListener(fragmentId)
            }
        }
    }

    /**
     * Shows the list of all tracks available.
     */
    fun navigateToAllSongs() {
        val fragment = findOrCreateFragment(CATEGORY_MUSIC, SongListFragment.Factory::newInstance)
        showFragment(CATEGORY_MUSIC, fragment)
    }

    /**
     * Shows the list of all albums.
     */
    fun navigateToAlbums() {
        val fragment =
            findOrCreateFragment(CATEGORY_ALBUMS, AlbumGridFragment.Factory::newInstance)
        showFragment(CATEGORY_ALBUMS, fragment)
    }

    /**
     * Shows the list of all artists.
     */
    fun navigateToArtists() {
        val fragment =
            findOrCreateFragment(CATEGORY_ARTISTS, ArtistListFragment.Factory::newInstance)
        showFragment(CATEGORY_ARTISTS, fragment)
    }

    /**
     * Shows the detail of a specific artist.
     * This includes its produced albums and tracks.
     *
     * @param artist The artist from whose details are to be displayed
     */
    fun navigateToArtistDetail(artist: MediaBrowserCompat.MediaItem) {
        val tag = requireNotNull(artist.mediaId)
        val fragment = findOrCreateFragment(tag) {
            ArtistDetailFragment.newInstance(artist)
        }

        showFragment(tag, fragment)
    }

    /**
     * Shows the list of user defined playlists.
     */
    fun navigateToPlaylists() {
        val fragment =
            findOrCreateFragment(CATEGORY_PLAYLISTS, PlaylistsFragment.Factory::newInstance)
        showFragment(CATEGORY_PLAYLISTS, fragment)
    }

    /**
     * Shows the list of tracks that are part of a specific playlist.
     *
     * @param playlist the playlist from whose tracks are to be displayed
     */
    fun navigateToPlaylistDetails(playlist: MediaBrowserCompat.MediaItem) {
        val tag =
            playlist.mediaId ?: throw IllegalArgumentException("Playlist should have a mediaId")
        val fragment = findOrCreateFragment(tag) {
            // Only user-defined playlists should be deletable
            val rootId = browseHierarchyOf(tag)[0]
            MembersFragment.newInstance(playlist, deletable = (CATEGORY_PLAYLISTS == rootId))
        }

        showFragment(tag, fragment)
    }

    /**
     * Shows the settings screen.
     */
    fun navigateToSettings() {
        val settingsActivity = Intent(activity, SettingsActivity::class.java)
        activity.startActivityForResult(settingsActivity, REQUEST_SETTINGS)
    }

    /**
     * Manually go back to the previous screen, if any.
     *
     * This is the same as pressing the back button except that the application won't be closed.
     */
    fun navigateBack() = fm.popBackStack()

    /**
     * Shows the view that correspond to a specific media id.
     *
     * @param mediaId The media id that represents the screen to display
     */
    fun navigateToMediaId(mediaId: String) {
        val root = browseHierarchyOf(mediaId)[0]
        when (root) {
            CATEGORY_MUSIC -> navigateToAllSongs()
            CATEGORY_ALBUMS -> navigateToAlbums()
            CATEGORY_ARTISTS -> navigateToArtists()
            CATEGORY_PLAYLISTS -> navigateToPlaylists()
            else -> error("Unsupported media ID: $mediaId")
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
        outState?.putString(KEY_FIRST_TAG, firstTag)
    }

    /**
     * Restores the state of this navigation controller from the specified bundle.
     *
     * @param savedInstanceState The bundle from [android.app.Activity.onRestoreInstanceState]
     */
    fun restoreState(savedInstanceState: Bundle?) {
        firstTag = savedInstanceState?.getString(KEY_FIRST_TAG)
    }

    /**
     * Retrieve a fragment with the specified tag from the fragment manager,
     * or create it if it does not exist.
     * @param tag the tag of the fragment to retrieve from the fragment manager
     * @param provider a function that provides the requested fragment if does not exist
     * @return the retrieved fragment, or the provided one if not in fragment manager
     */
    private inline fun findOrCreateFragment(tag: String, provider: () -> androidx.fragment.app.Fragment) =
        fm.findFragmentByTag(tag) ?: provider()

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
    private fun showFragment(tag: String, fragment: androidx.fragment.app.Fragment) {
        if (fm.findFragmentById(containerId) != null) {
            // Is the fragment to show already on the back stack ?
            if (fm.findFragmentByTag(tag) != null) {
                if (tag == firstTag) {
                    // Pop all transactions if the fragment to show is the first one added
                    fm.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                } else {
                    // Pop until showing the fragment
                    fm.popBackStack(tag, 0)
                }
            } else {
                fm.beginTransaction()
                    .replace(containerId, fragment, tag)
                    .setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .addToBackStack(tag)
                    .commit()
            }
        } else {
            // This is the first fragment to be shown. Just add it.
            firstTag = tag
            fm.beginTransaction()
                .add(containerId, fragment, tag)
                .commitAllowingStateLoss()

            val fragmentId = fragment.arguments!!.getInt(FRAGMENT_ID)
            routeChangeListener(fragmentId)
        }
    }
}
