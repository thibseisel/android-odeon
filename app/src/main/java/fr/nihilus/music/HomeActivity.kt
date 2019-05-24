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
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import fr.nihilus.music.base.BaseActivity
import fr.nihilus.music.extensions.lockDrawer
import fr.nihilus.music.extensions.observeK
import fr.nihilus.music.library.MusicLibraryViewModel
import fr.nihilus.music.library.NavigationController
import fr.nihilus.music.library.nowplaying.NowPlayingFragment
import fr.nihilus.music.media.permissions.PermissionChecker
import fr.nihilus.music.ui.EXTERNAL_STORAGE_REQUEST
import fr.nihilus.music.ui.requestExternalStoragePermission
import fr.nihilus.music.ui.ConfirmDialogFragment
import kotlinx.android.synthetic.main.activity_home.*
import javax.inject.Inject

private const val ACTION_RANDOM = "fr.nihilus.music.ACTION_RANDOM"

class HomeActivity : BaseActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    @Inject lateinit var permissions: PermissionChecker
    @Inject lateinit var router: NavigationController

    private lateinit var drawerToggle: ActionBarDrawerToggle

    private val viewModel: MusicLibraryViewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(this, viewModelFactory)[MusicLibraryViewModel::class.java]
    }

    private lateinit var bottomSheet: BottomSheetBehavior<*>
    private lateinit var playerFragment: NowPlayingFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        viewModel.toolbarTitle.observeK(this, toolbar::setTitle)

        setupNavigationDrawer()
        setupPlayerView()

        if (savedInstanceState == null) {
            if (!permissions.canWriteToExternalStorage) {
                // Load a fragment depending on the intent that launched that activity (shortcuts)
                if (!handleIntent(intent)) {
                    // If intent is not handled, display all tracks
                    router.navigateToAllSongs()
                }
            } else this.requestExternalStoragePermission()
        }
    }

    override fun onAttachFragment(fragment: androidx.fragment.app.Fragment?) {
        super.onAttachFragment(fragment)
        if (fragment is NowPlayingFragment) {
            playerFragment = fragment
            fragment.setOnRequestPlayerExpansionListener { shouldCollapse ->
                bottomSheet.state =
                        if (shouldCollapse) BottomSheetBehavior.STATE_COLLAPSED
                        else BottomSheetBehavior.STATE_EXPANDED
            }
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
        router.routeChangeListener = nav_drawer::setCheckedItem
    }

    private fun setupPlayerView() {
        bottomSheet = BottomSheetBehavior.from(player_container)

        // Show / hide BottomSheet on startup without an animation
        setInitialBottomSheetVisibility(viewModel.playerSheetVisible.value)
        viewModel.playerSheetVisible.observeK(this, this::onSheetVisibilityChanged)

        viewModel.playerError.observeK(this) { playerErrorEvent ->
            playerErrorEvent?.handle { errorMessage ->
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }

        bottomSheet.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_SETTLING ||
                    newState == BottomSheetBehavior.STATE_DRAGGING) {
                    return
                }

                val isExpandedOrExpanding = newState != BottomSheetBehavior.STATE_COLLAPSED
                        && newState != BottomSheetBehavior.STATE_HIDDEN
                playerFragment.setCollapsed(!isExpandedOrExpanding)
                drawer_layout.lockDrawer(isExpandedOrExpanding)
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
                router.navigateToAllSongs()
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

    /**
     * Show or hide the player view depending on the passed playback state.
     * This method is meant to be called only once to show or hide player view without animation.
     */
    private fun setInitialBottomSheetVisibility(shouldShow: Boolean?) {
        bottomSheet.peekHeight = if (shouldShow == true) {
            resources.getDimensionPixelSize(R.dimen.playerview_height)
        } else {
            resources.getDimensionPixelSize(R.dimen.playerview_hidden_height)
        }
    }

    private fun onSheetVisibilityChanged(sheetVisible: Boolean?) {
        if (sheetVisible == true) {
            if (bottomSheet.isHideable || bottomSheet.peekHeight == 0) {
                // Take action to show BottomSheet only if it is hidden
                bottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
                player_container.post { bottomSheet.isHideable = false }
                val playerViewHeight = resources.getDimensionPixelSize(R.dimen.playerview_height)
                bottomSheet.peekHeight = playerViewHeight
            }
        } else {
            bottomSheet.isHideable = true
            bottomSheet.state = BottomSheetBehavior.STATE_HIDDEN

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
        viewModel.playAllShuffled()
    }
}
