package fr.nihilus.music;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import fr.nihilus.music.library.BrowserViewModel;
import fr.nihilus.music.library.NavigationController;
import fr.nihilus.music.library.ViewModelFactory;
import fr.nihilus.music.settings.PreferenceDao;
import fr.nihilus.music.settings.SettingsActivity;
import fr.nihilus.music.utils.MediaID;
import fr.nihilus.music.utils.PermissionUtil;

@SuppressWarnings("ConstantConditions")
public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, HasSupportFragmentInjector {

    private static final int REQUEST_SETTINGS = 42;
    private static final String ACTION_ALBUMS = "fr.nihilus.music.ACTION_ALBUMS";
    private static final String ACTION_RANDOM = "fr.nihilus.music.ACTION_RANDOM";
    private static final String TAG = "HomeActivity";

    @Inject DispatchingAndroidInjector<Fragment> dispatchingFragmentInjector;
    @Inject PreferenceDao mPrefs;
    @Inject NavigationController mRouter;
    @Inject ViewModelFactory mFactory;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView mNavigationView;

    private BrowserViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        setupNavigationDrawer();

        mViewModel = ViewModelProviders.of(this, mFactory).get(BrowserViewModel.class);
        mViewModel.connect();

        if (savedInstanceState == null) {
            if (PermissionUtil.hasExternalStoragePermission(this)) {
                // Load a fragment depending on the intent that launched that activity (shortcuts)
                if (!handleIntent(getIntent())) {
                    // If intent is not handled, load default fragment
                    mNavigationView.setCheckedItem(R.id.action_all);
                    mRouter.navigateToAllSongs();
                }
            } else PermissionUtil.requestExternalStoragePermission(this);
        }
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * Allow dispatching media key events to the playback service for API < 21.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                MediaControllerCompat controller = MediaControllerCompat.getMediaController(this);
                if (controller != null) {
                    controller.dispatchMediaButtonEvent(event);
                    return true;
                } else return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.action_all:
                mRouter.navigateToAllSongs();
                return true;
            case R.id.action_albums:
                mRouter.navigateToAlbums();
                return true;
            case R.id.action_artists:
                mRouter.navigateToArtists();
                return true;
            case R.id.action_playlist:
                mRouter.navigateToPlaylists();
                return true;
            case R.id.action_settings:
                Intent settingsActivity = new Intent(this, SettingsActivity.class);
                startActivityForResult(settingsActivity, REQUEST_SETTINGS);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Create and populate the Navigation Drawer.
     */
    private void setupNavigationDrawer() {
        mDrawerLayout = findViewById(R.id.drawerLayout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_opened, R.string.drawer_closed);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mNavigationView = findViewById(R.id.navDrawer);
        mNavigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        onOptionsItemSelected(item);
        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Called when an activity launched by this one exits and returns a result.
     * This allows this activity to recreate itself if a preference that changed
     * in {@link SettingsActivity} affects the visual state (such as the nightmode preference).
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == REQUEST_SETTINGS) && (resultCode == RESULT_OK)) {
            // SettingsActivity asks this activity to restart in order to apply preference changes.
            recreate();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Called when receiving an intent while the Activity is alive.
     * This is intended to handle actions relative to launcher shortcuts (API25+).
     *
     * @param intent the new intent that was started for the activity
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PermissionUtil.EXTERNAL_STORAGE_REQUEST) {
            // Whether it has permission or not, load fragment into interface
            if (!handleIntent(getIntent())) {
                mNavigationView.setCheckedItem(R.id.action_all);
                mRouter.navigateToAllSongs();
            }
        }
    }

    /**
     * Perform an action depending on the received intent.
     * This is intended to handle actions relative to launcher shortcuts (API 25+).
     *
     * @param intent the intent that started this activity, or was received later
     * @return true if intent was handled, false otherwise
     */
    private boolean handleIntent(@Nullable Intent intent) {
        Log.d(TAG, "handleIntent: " + intent.getAction());
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_RANDOM:
                    startRandomMix();
                    return false;
                case ACTION_ALBUMS:
                    mNavigationView.setCheckedItem(R.id.action_albums);
                    mRouter.navigateToAlbums();
                    return true;
            }
        }
        return false;
    }

    private void startRandomMix() {
        mViewModel.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL);
        mViewModel.playFromMediaId(MediaID.ID_MUSIC);
    }

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return dispatchingFragmentInjector;
    }
}
