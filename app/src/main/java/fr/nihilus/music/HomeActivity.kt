package fr.nihilus.music

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.math.MathUtils
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
import fr.nihilus.music.library.BrowserViewModel
import fr.nihilus.music.library.NavigationController
import fr.nihilus.music.library.ViewModelFactory
import fr.nihilus.music.settings.PreferenceDao
import fr.nihilus.music.settings.SettingsActivity
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
            KeyEvent.KEYCODE_MEDIA_PLAY -> mViewModel.dispatchMediaButtonEvent(event)
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

        mViewModel.currentMetadata.observe(this, Observer(mPlayerView::updateMetadata))
        mViewModel.playbackState.observe(this, Observer(mPlayerView::updatePlaybackState))
        mViewModel.shuffleMode.observe(this, Observer {
            mPlayerView.setShuffleMode(it ?: PlaybackStateCompat.SHUFFLE_MODE_NONE)
        })

        mViewModel.repeatMode.observe(this, Observer {
            mPlayerView.setRepeatMode(it ?: PlaybackStateCompat.REPEAT_MODE_NONE)
        })

        BottomSheetBehavior.from(mPlayerView)
                .setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        val opacity = MathUtils.clamp(slideOffset, 0.0f, 1.0f)
                        mBottomSheet.scrimOpacity = opacity * 0.5f
                        mCoordinator.requestLayout()
                    }

                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        when (newState) {
                            BottomSheetBehavior.STATE_COLLAPSED -> mPlayerView.setExpanded(false)
                            BottomSheetBehavior.STATE_EXPANDED -> mPlayerView.setExpanded(true)
                        }
                    }
                })
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        onOptionsItemSelected(item)
        mDrawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    /**
     * Called when an activity launched by this one exits and returns a result.
     * This allows this activity to recreate itself if a preference that changed
     * in [SettingsActivity] affects the visual state (such as the nightmode preference).
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SETTINGS) {
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
        }
    }

    private fun showHomeScreen() {
        // Load the startup fragment defined in shared preferences
        val mediaId = mPrefs.startupScreenMediaId
        mRouter.navigateToMediaId(mediaId)
    }

    /**
     * Perform an action depending on the received intent.
     * This is intended to handle actions relative to launcher shortcuts (API 25+).
     *
     * @param intent the intent that started this activity, or was received later
     * @return true if intent was handled, false otherwise
     */
    private fun handleIntent(intent: Intent?): Boolean {
        Log.d(TAG, "handleIntent: " + intent?.action)
        if (intent != null && intent.action != null) {
            when (intent.action) {
                ACTION_RANDOM -> {
                    startRandomMix()
                    return false
                }
                ACTION_ALBUMS -> {
                    mNavigationView.setCheckedItem(R.id.action_albums)
                    mRouter.navigateToAlbums()
                    return true
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
        mViewModel.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
        mViewModel.playFromMediaId(MediaID.ID_MUSIC)
    }

    override fun onActionPlay() {
        mViewModel.play()
    }

    override fun onActionPause() {
        mViewModel.pause()
    }

    override fun onSeek(position: Long) {
        mViewModel.seekTo(position)
    }

    override fun onSkipToPrevious() {
        mViewModel.skipToPrevious()
    }

    override fun onSkipToNext() {
        mViewModel.skipToNext()
    }

    override fun onRepeatModeChanged(newMode: Int) {
        mViewModel.setRepeatMode(newMode)
    }

    override fun onShuffleModeChanged(newMode: Int) {
        mViewModel.setShuffleMode(newMode)
    }

    override fun supportFragmentInjector() = dispatchingFragmentInjector

    private companion object {
        private const val REQUEST_SETTINGS = 42
        private const val ACTION_ALBUMS = "fr.nihilus.music.ACTION_ALBUMS"
        private const val ACTION_RANDOM = "fr.nihilus.music.ACTION_RANDOM"
        private const val TAG = "HomeActivity"
    }
}
