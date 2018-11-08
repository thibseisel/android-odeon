/*
 * Copyright 2018 Thibault Seisel
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

package fr.nihilus.music

import android.Manifest
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import fr.nihilus.music.client.BrowserViewModel
import fr.nihilus.music.client.NavigationController
import fr.nihilus.music.client.REQUEST_SETTINGS
import fr.nihilus.music.media.CATEGORY_MUSIC
import fr.nihilus.music.media.utils.EXTERNAL_STORAGE_REQUEST
import fr.nihilus.music.media.utils.hasExternalStoragePermission
import fr.nihilus.music.media.utils.requestExternalStoragePermission
import fr.nihilus.music.settings.SettingsActivity
import fr.nihilus.music.settings.UiSettings
import fr.nihilus.music.ui.NowPlayingViewModel
import fr.nihilus.music.utils.ConfirmDialogFragment
import kotlinx.android.synthetic.main.activity_home.*
import javax.inject.Inject

class HomeActivity : BaseActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    @Inject lateinit var prefs: UiSettings
    @Inject lateinit var router: NavigationController

    private lateinit var drawerToggle: ActionBarDrawerToggle

    private lateinit var bottomSheet: BottomSheetBehavior<*>
    private lateinit var viewModel: BrowserViewModel
    private lateinit var playerViewModel: NowPlayingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        setupNavigationDrawer()

        playerViewModel = ViewModelProviders.of(this, viewModelFactory)[NowPlayingViewModel::class.java]

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(BrowserViewModel::class.java)
        viewModel.connect()

        setupPlayerView()

        if (savedInstanceState == null) {
            if (this.hasExternalStoragePermission()) {
                // Load a fragment depending on the intent that launched that activity (shortcuts)
                if (!handleIntent(intent)) {
                    // If intent is not handled, load default fragment
                    showHomeScreen()
                }
            } else this.requestExternalStoragePermission()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
    }

    /**
     * Allow dispatching media key events to the playback service for API < 21.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY -> viewModel.post {
                it.dispatchMediaButtonEvent(event)
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true
        }

        when (item.itemId) {
            R.id.action_all -> {
                router.navigateToAllSongs()
                return true
            }
            R.id.action_albums -> {
                router.navigateToAlbums()
                return true
            }
            R.id.action_artists -> {
                router.navigateToArtists()
                return true
            }
            R.id.action_playlist -> {
                router.navigateToPlaylists()
                return true
            }
            R.id.action_settings -> {
                router.navigateToSettings()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Create and populate the Navigation Drawer.
     */
    private fun setupNavigationDrawer() {
        drawerToggle = ActionBarDrawerToggle(
            this, drawer_layout,
            R.string.drawer_opened, R.string.drawer_closed
        )
        drawer_layout.addDrawerListener(drawerToggle)

        nav_drawer.setNavigationItemSelectedListener(this)

        router.routeChangeListener = { fragmentId ->
            nav_drawer.setCheckedItem(fragmentId)
        }
    }

    private fun setupPlayerView() {
        bottomSheet = BottomSheetBehavior.from(player_container)

        // Show / hide BottomSheet on startup without an animation
        setInitialBottomSheetVisibility(viewModel.playbackState.value)

        viewModel.playbackState.observe(this, Observer { newState ->
            togglePlayerVisibility(newState)
        })

        viewModel.playerError.observe(this, Observer { errorMessage ->
            if (errorMessage != null) {
                Toast.makeText(this@HomeActivity, errorMessage, Toast.LENGTH_LONG).show()
            }
        })

        bottomSheet.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_SETTLING ||
                    newState == BottomSheetBehavior.STATE_DRAGGING) {
                    return
                }

                val isExpandedOrExpanding = newState != BottomSheetBehavior.STATE_COLLAPSED
                        && newState != BottomSheetBehavior.STATE_HIDDEN

                playerViewModel.markAsExpanded(isExpandedOrExpanding)
                with(drawer_layout) {
                    requestDisallowInterceptTouchEvent(isExpandedOrExpanding)
                    setDrawerLockMode(
                        if (isExpandedOrExpanding) DrawerLayout.LOCK_MODE_LOCKED_CLOSED
                        else DrawerLayout.LOCK_MODE_UNLOCKED
                    )
                }
            }
        })
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        onOptionsItemSelected(item)
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    /**
     * Called when the back button is pressed.
     * This will close the navigation drawer if open, collapse the player view if expanded,
     * or otherwise follow the default behavior (pop fragment back stack or finish activity).
     */
    override fun onBackPressed() = when {
        drawer_layout.isDrawerOpen(GravityCompat.START) ->
            drawer_layout.closeDrawer(GravityCompat.START)

        bottomSheet.state == BottomSheetBehavior.STATE_EXPANDED ->
            bottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED

        else -> super.onBackPressed()
    }

    /**
     * Called when an activity launched by this one exits and returns a result.
     * This allows this activity to recreate itself if a preference that changed
     * in [SettingsActivity] affects the visual state (such as the night mode preference).
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SETTINGS) {
            recreate()
        }
    }

    /**
     * Called when receiving an intent while the Activity is alive.
     * This is intended to handle actions relative to launcher shortcuts (API25+).
     *
     * @param intent the new intent that was started for the activity
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == EXTERNAL_STORAGE_REQUEST) {

            // Whether it has permission or not, load fragment into interface
            if (!handleIntent(intent)) {
                showHomeScreen()
            }

            // Show an informative dialog message if permission is not granted
            // and user has not checked "Don't ask again".
            if (grantResults[0] == PackageManager.PERMISSION_DENIED &&
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )) {
                ConfirmDialogFragment.newInstance(
                    null, 0,
                    message = getString(R.string.external_storage_permission_rationale),
                    positiveButton = R.string.ok
                ).show(supportFragmentManager, null)
            }
        }
    }

    private fun showHomeScreen() {
        // Load the startup fragment defined in shared preferences
        val mediaId = prefs.startupScreenMediaId
        router.navigateToMediaId(mediaId)
    }

    /**
     * Show or hide the player view depending on the passed playback state.
     * This method is meant to be called only once to show or hide player view without animation.
     */
    private fun setInitialBottomSheetVisibility(state: PlaybackStateCompat?) {
        bottomSheet.peekHeight = if (state == null
            || state.state == PlaybackStateCompat.STATE_NONE
            || state.state == PlaybackStateCompat.STATE_STOPPED) {
            resources.getDimensionPixelSize(R.dimen.playerview_hidden_height)
        } else {
            resources.getDimensionPixelSize(R.dimen.playerview_height)
        }
    }

    /**
     * Show or hide the player view depending on the passed playback state.
     * If the playback state is undefined or stopped, the player view will be hidden.
     *
     * @param state The most recent playback state
     */
    private fun togglePlayerVisibility(state: PlaybackStateCompat?) {
        if (state == null
            || state.state == PlaybackStateCompat.STATE_NONE
            || state.state == PlaybackStateCompat.STATE_STOPPED) {
            bottomSheet.isHideable = true
            bottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
            container.setPadding(0, 0, 0, 0)

        } else if (bottomSheet.isHideable || bottomSheet.peekHeight == 0) {
            // Take action to show BottomSheet only if it is hidden
            bottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
            player_container.post { bottomSheet.isHideable = false }
            val playerViewHeight = resources.getDimensionPixelSize(R.dimen.playerview_height)
            container.setPadding(0, 0, 0, playerViewHeight)
            bottomSheet.peekHeight = playerViewHeight
        }
    }

    /**
     * Perform an action depending on the received intent.
     * This is intended to handle actions relative to launcher shortcuts (API 25+).
     *
     * @param intent the intent that started this activity, or was received later
     * @return true if intent was handled, false otherwise
     */
    private fun handleIntent(intent: Intent?): Boolean {
            when (intent?.action) {
                ACTION_RANDOM -> {
                    startRandomMix()
                    // Intent is purposely marked as not handled to trigger home screen display
                    return false
                }
                ACTION_ALBUMS -> {
                    nav_drawer.setCheckedItem(R.id.action_albums)
                    router.navigateToAlbums()
                    return true
                }
                ACTION_ARTISTS -> {
                    nav_drawer.setCheckedItem(R.id.action_artists)
                    router.navigateToArtists()
                    return true
                }
                ACTION_PLAYLISTS -> {
                    nav_drawer.setCheckedItem(R.id.action_playlist)
                    router.navigateToPlaylists()
                    return true
                }
                Intent.ACTION_MAIN -> {
                    // Activity has been started normally from the launcher.
                    // Prepare playback when connection is established.
                    viewModel.post { controller ->
                        val playbackState = controller.playbackState
                        if (playbackState == null
                            || playbackState.state == PlaybackStateCompat.STATE_NONE
                            || playbackState.state == PlaybackStateCompat.STATE_STOPPED) {
                            controller.transportControls.prepare()
                        }
                    }
                }
            }

        return false
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        router.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        router.restoreState(savedInstanceState)
        super.onRestoreInstanceState(savedInstanceState)
    }

    private fun startRandomMix() {
        viewModel.post { controller ->
            with(controller.transportControls) {
                setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL)
                playFromMediaId(CATEGORY_MUSIC, null)
            }
        }
    }

    private companion object {
        private const val ACTION_ALBUMS = "fr.nihilus.music.ACTION_ALBUMS"
        private const val ACTION_RANDOM = "fr.nihilus.music.ACTION_RANDOM"
        private const val ACTION_ARTISTS = "fr.nihilus.music.ACTION_ARTISTS"
        private const val ACTION_PLAYLISTS = "fr.nihilus.music.ACTION_PLAYLISTS"
    }
}
