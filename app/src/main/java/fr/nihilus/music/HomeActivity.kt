/*
 * Copyright 2017 Thibault Seisel
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
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import dagger.android.AndroidInjection
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import fr.nihilus.music.client.BrowserViewModel
import fr.nihilus.music.client.NavigationController
import fr.nihilus.music.client.ViewModelFactory
import fr.nihilus.music.settings.PreferenceDao
import fr.nihilus.music.settings.SettingsActivity
import fr.nihilus.music.utils.ConfirmDialogFragment
import fr.nihilus.music.utils.MediaID
import fr.nihilus.music.utils.PermissionUtil
import fr.nihilus.music.view.PlayerView
import fr.nihilus.music.view.ScrimBottomSheetBehavior
import javax.inject.Inject

class HomeActivity : AppCompatActivity(),
        HasSupportFragmentInjector,
        NavigationView.OnNavigationItemSelectedListener,
        PlayerView.EventListener {

    @Inject lateinit var dispatchingFragmentInjector: DispatchingAndroidInjector<Fragment>
    @Inject lateinit var mPrefs: PreferenceDao
    @Inject lateinit var mRouter: NavigationController
    @Inject lateinit var mFactory: ViewModelFactory

    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var mDrawerToggle: ActionBarDrawerToggle
    private lateinit var mNavigationView: NavigationView
    private lateinit var mPlayerView: PlayerView
    private lateinit var mCoordinator: CoordinatorLayout
    private lateinit var mContainer: View

    private lateinit var mBottomSheet: ScrimBottomSheetBehavior<PlayerView>
    private lateinit var mViewModel: BrowserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        mCoordinator = findViewById(R.id.coordinatorLayout)
        setSupportActionBar(findViewById(R.id.toolbar))
        setupNavigationDrawer()

        mViewModel = ViewModelProviders.of(this, mFactory).get(BrowserViewModel::class.java)
        mViewModel.connect()

        mContainer = findViewById(R.id.container)
        setupPlayerView()

        if (savedInstanceState == null) {
            if (PermissionUtil.hasExternalStoragePermission(this)) {
                // Load a fragment depending on the intent that launched that activity (shortcuts)
                if (!handleIntent(intent)) {
                    // If intent is not handled, load default fragment
                    showHomeScreen()
                }
            } else PermissionUtil.requestExternalStoragePermission(this)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        mDrawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mDrawerToggle.onConfigurationChanged(newConfig)
    }

    /**
     * Allow dispatching media key events to the playback service for API < 21.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY -> mViewModel.post { controller ->
                controller.dispatchMediaButtonEvent(event)
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true
        }

        when (item.itemId) {
            R.id.action_home -> {
                mRouter.navigateToHome()
                return true
            }
            R.id.action_all -> {
                mRouter.navigateToAllSongs()
                return true
            }
            R.id.action_albums -> {
                mRouter.navigateToAlbums()
                return true
            }
            R.id.action_artists -> {
                mRouter.navigateToArtists()
                return true
            }
            R.id.action_playlist -> {
                mRouter.navigateToPlaylists()
                return true
            }
            R.id.action_settings -> {
                val settingsActivity = Intent(this, SettingsActivity::class.java)
                startActivityForResult(settingsActivity, REQUEST_SETTINGS)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Create and populate the Navigation Drawer.
     */
    private fun setupNavigationDrawer() {
        mDrawerLayout = findViewById(R.id.drawerLayout)
        mDrawerToggle = ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_opened, R.string.drawer_closed)
        mDrawerLayout.addDrawerListener(mDrawerToggle)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        mNavigationView = findViewById(R.id.navDrawer)
        mNavigationView.setNavigationItemSelectedListener(this)

        mRouter.routeChangeListener = { fragmentId ->
            mNavigationView.setCheckedItem(fragmentId)
        }
    }

    private fun setupPlayerView() {
        mPlayerView = findViewById(R.id.playerView)
        mPlayerView.setEventListener(this)
        mBottomSheet = ScrimBottomSheetBehavior.from(mPlayerView)

        // Show / hide BottomSheet on startup without an animation
        setInitialBottomSheetVisibility(mViewModel.playbackState.value)

        mViewModel.currentMetadata.observe(this, Observer(mPlayerView::updateMetadata))
        mViewModel.shuffleMode.observe(this, Observer {
            mPlayerView.setShuffleMode(it ?: PlaybackStateCompat.SHUFFLE_MODE_NONE)
        })

        mViewModel.repeatMode.observe(this, Observer {
            mPlayerView.setRepeatMode(it ?: PlaybackStateCompat.REPEAT_MODE_NONE)
        })

        mViewModel.playbackState.observe(this, Observer { newState ->
            mPlayerView.updatePlaybackState(newState)
            togglePlayerVisibility(newState)
        })

        BottomSheetBehavior.from(mPlayerView)
                .setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        mBottomSheet.scrimOpacity = slideOffset.coerceAtLeast(0.0f) * 0.5f
                        bottomSheet.requestLayout()
                    }

                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState != BottomSheetBehavior.STATE_COLLAPSED
                                && newState != BottomSheetBehavior.STATE_HIDDEN) {
                            mPlayerView.setExpanded(true)
                            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                        } else {
                            mPlayerView.setExpanded(false)
                            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                        }
                    }
                })
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        onOptionsItemSelected(item)
        mDrawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (mBottomSheet.state != BottomSheetBehavior.STATE_EXPANDED) {
            super.onBackPressed()
        } else {
            // Collapses the player view if expanded
            mBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
        }

    }

    /**
     * Called when an activity launched by this one exits and returns a result.
     * This allows this activity to recreate itself if a preference that changed
     * in [SettingsActivity] affects the visual state (such as the night mode preference).
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SETTINGS) {
            // FIXME Not called after returning to activity
            delegate.applyDayNight()
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == PermissionUtil.EXTERNAL_STORAGE_REQUEST) {

            // Whether it has permission or not, load fragment into interface
            if (!handleIntent(intent)) {
                showHomeScreen()
            }

            // Show an informative dialog message if permission is not granted
            // and user has not checked "Don't ask again".
            if (grantResults[0] == PackageManager.PERMISSION_DENIED &&
                    ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ConfirmDialogFragment.newInstance(null, 0,
                        message = getString(R.string.external_storage_permission_rationale),
                        positiveButton = R.string.ok
                ).show(supportFragmentManager, null)
            }
        }
    }

    private fun showHomeScreen() {
        // Load the startup fragment defined in shared preferences
        val mediaId = mPrefs.startupScreenMediaId
        mRouter.navigateToMediaId(mediaId)
    }

    /**
     * Show or hide the player view depending on the passed playback state.
     * This method is meant to be called only once to show or hide player view without animation.
     */
    private fun setInitialBottomSheetVisibility(state: PlaybackStateCompat?) {
        mBottomSheet.peekHeight = if (state == null
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
        Log.d(TAG, "Is Hidden ? ${mBottomSheet.state == BottomSheetBehavior.STATE_HIDDEN}")
        if (state == null
                || state.state == PlaybackStateCompat.STATE_NONE
                || state.state == PlaybackStateCompat.STATE_STOPPED) {
            mBottomSheet.isHideable = true
            mBottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
            mContainer.setPadding(0, 0, 0, 0)

        } else if (mBottomSheet.isHideable
                || mBottomSheet.peekHeight == 0) {
            // Take action to show BottomSheet only if it is hidden
            mBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
            mPlayerView.post { mBottomSheet.isHideable = false }
            val playerViewHeight = resources.getDimensionPixelSize(R.dimen.playerview_height)
            mContainer.setPadding(0, 0, 0, playerViewHeight)
            mBottomSheet.peekHeight = playerViewHeight
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
        if (intent != null && intent.action != null) {
            when (intent.action) {
                ACTION_RANDOM -> {
                    startRandomMix()
                    // Intent is purposely marked as not handled to trigger home screen display
                    return false
                }
                ACTION_ALBUMS -> {
                    mNavigationView.setCheckedItem(R.id.action_albums)
                    mRouter.navigateToAlbums()
                    return true
                }
                ACTION_ARTISTS -> {
                    mNavigationView.setCheckedItem(R.id.action_artists)
                    mRouter.navigateToArtists()
                    return true
                }
                ACTION_PLAYLISTS -> {
                    mNavigationView.setCheckedItem(R.id.action_playlist)
                    mRouter.navigateToPlaylists()
                    return true
                }
                Intent.ACTION_MAIN -> {
                    // Activity has been started normally from the launcher.
                    // Prepare playback when connection is established.
                    mViewModel.post { controller ->
                        val playbackState = controller.playbackState
                        if (playbackState == null
                                || playbackState.state == PlaybackStateCompat.STATE_NONE
                                || playbackState.state == PlaybackStateCompat.STATE_STOPPED) {
                            controller.transportControls.prepare()
                        }
                    }
                }
            }
        }

        return false
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        mRouter.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        mRouter.restoreState(savedInstanceState)
        super.onRestoreInstanceState(savedInstanceState)
    }

    private fun startRandomMix() {
        mViewModel.post { controller ->
            with(controller.transportControls) {
                setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL)
                playFromMediaId(MediaID.ID_MUSIC, null)
            }
        }
    }

    override fun onActionPlay() {
        mViewModel.post { it.transportControls.play() }
    }

    override fun onActionPause() {
        mViewModel.post { it.transportControls.pause() }
    }

    override fun onSeek(position: Long) {
        mViewModel.post { it.transportControls.seekTo(position) }
    }

    override fun onSkipToPrevious() {
        mViewModel.post { it.transportControls.skipToPrevious() }
    }

    override fun onSkipToNext() {
        mViewModel.post { it.transportControls.skipToNext() }
    }

    override fun onRepeatModeChanged(newMode: Int) {
        mViewModel.post { it.transportControls.setRepeatMode(newMode) }
    }

    override fun onShuffleModeChanged(newMode: Int) {
        mViewModel.post { it.transportControls.setShuffleMode(newMode) }
    }

    override fun supportFragmentInjector() = dispatchingFragmentInjector

    private companion object {
        private const val TAG = "HomeActivity"
        private const val REQUEST_SETTINGS = 42
        private const val ACTION_ALBUMS = "fr.nihilus.music.ACTION_ALBUMS"
        private const val ACTION_RANDOM = "fr.nihilus.music.ACTION_RANDOM"
        private const val ACTION_ARTISTS = "fr.nihilus.music.ACTION_ARTISTS"
        private const val ACTION_PLAYLISTS = "fr.nihilus.music.ACTION_PLAYLISTS"
    }
}
