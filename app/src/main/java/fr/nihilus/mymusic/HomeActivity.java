package fr.nihilus.mymusic;

import android.app.SearchManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.ImageViewTarget;

import java.util.List;

import fr.nihilus.mymusic.MediaBrowserFragment.ConnectedCallback;
import fr.nihilus.mymusic.palette.BottomPaletteTranscoder;
import fr.nihilus.mymusic.palette.PaletteBitmap;
import fr.nihilus.mymusic.provider.SetupService;
import fr.nihilus.mymusic.service.MusicService;
import fr.nihilus.mymusic.settings.Prefs;
import fr.nihilus.mymusic.settings.SettingsActivity;
import fr.nihilus.mymusic.ui.albums.AlbumGridFragment;
import fr.nihilus.mymusic.ui.artists.ArtistDetailFragment;
import fr.nihilus.mymusic.ui.artists.ArtistsFragment;
import fr.nihilus.mymusic.ui.playlist.PlaylistsFragment;
import fr.nihilus.mymusic.ui.songs.SongListFragment;
import fr.nihilus.mymusic.utils.MediaID;
import fr.nihilus.mymusic.utils.PermissionUtil;
import fr.nihilus.mymusic.view.PlayerView;

@SuppressWarnings("ConstantConditions")
public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "HomeActivity";

    public static final int REQUEST_SETTINGS = 42;
    public static final String ACTION_ALBUMS = "fr.nihilus.mymusic.ACTION_ALBUMS";
    public static final String ACTION_RANDOM = "fr.nihilus.mymusic.ACTION_RANDOM";

    private static final String KEY_DAILY_SONG = "daily_song";
    private static final String KEY_BOTTOMSHEET_STATE = "bottomsheet_state";

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView mNavigationView;
    private PlayerView mPlayerView;
    private MediaItem mDaily;

    /**
     * Called when MediaController has been attached to this Activity.
     * Associate this controller to the PlayerView.
     */
    private final ConnectedCallback mConnectionCallback = new ConnectedCallback() {
        @Override
        public void onConnected() {
            MediaControllerCompat controller = MediaControllerCompat
                    .getMediaController(HomeActivity.this);
            mPlayerView.setMediaController(controller);
        }
    };

    /**
     * Starts a random mix of all music as soon as the MediaController
     * has been attached to the Activity.
     */
    private final ConnectedCallback mStartRandomMix = new ConnectedCallback() {
        @Override
        public void onConnected() {
            MediaControllerCompat controller = MediaControllerCompat.getMediaController(HomeActivity.this);
            if (controller != null) {
                Bundle args = new Bundle(1);
                args.putBoolean(MusicService.EXTRA_RANDOM_ENABLED, true);
                controller.getTransportControls()
                        .sendCustomAction(MusicService.CUSTOM_ACTION_RANDOM, args);
                controller.getTransportControls().playFromMediaId(MediaID.ID_MUSIC, null);
            }
        }
    };

    /**
     * Called when the daily song is available.
     * Display those informations as the Navigation Drawer's header.
     */
    private final SubscriptionCallback mDailySubscription = new SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, List<MediaItem> children) {
            if (children.size() > 0) {
                mDaily = children.get(0);
                prepareHeaderView(mDaily);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        setupNavigationDrawer();
        setupPlayerView();

        if (savedInstanceState == null) {
            if (PermissionUtil.hasExternalStoragePermission(this)) {
                loadDailySong();
                // Load a fragment depending on the intent that launched that activity (shortcuts)
                if (!handleIntent(getIntent())) {
                    // If intent is not handled, load default fragment
                    mNavigationView.setCheckedItem(R.id.action_all);
                    swapFragment(new SongListFragment());
                }
            } else PermissionUtil.requestExternalStoragePermission(this);
        } else {
            mDaily = savedInstanceState.getParcelable(KEY_DAILY_SONG);
            prepareHeaderView(mDaily);
            final int bottomSheetState = savedInstanceState.getInt(KEY_BOTTOMSHEET_STATE,
                    BottomSheetBehavior.STATE_COLLAPSED);
            BottomSheetBehavior.from(mPlayerView).setState(bottomSheetState);
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_DAILY_SONG, mDaily);
        outState.putInt(KEY_BOTTOMSHEET_STATE, BottomSheetBehavior.from(mPlayerView).getState());
    }

    @Override
    protected void onDestroy() {
        mPlayerView.setMediaController(null);
        super.onDestroy();
    }

    private void setupPlayerView() {
        mPlayerView = (PlayerView) findViewById(R.id.playerView);
        MediaBrowserFragment.getInstance(getSupportFragmentManager())
                .doWhenConnected(mConnectionCallback);
        BottomSheetBehavior.from(mPlayerView)
                .setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {

                    }

                    @Override
                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                        mPlayerView.setHeaderOpacity(1 - slideOffset);
                    }
                });
    }

    /**
     * Ask the MediaBrowser to fetch information about the daily song.
     * The daily song changes every time the app is open.
     */
    private void loadDailySong() {
        MediaBrowserFragment.getInstance(getSupportFragmentManager())
                .subscribe(MediaID.ID_DAILY, mDailySubscription);
    }

    /**
     * Create and populate the Navigation Drawer.
     */
    private void setupNavigationDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_opened, R.string.drawer_closed);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mNavigationView = (NavigationView) findViewById(R.id.navDrawer);
        mNavigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        getSupportFragmentManager().popBackStackImmediate(ArtistDetailFragment.BACKSTACK_ENTRY,
                FragmentManager.POP_BACK_STACK_INCLUSIVE);
        onOptionsItemSelected(item);
        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        final SearchView searchView = (SearchView) MenuItemCompat
                .getActionView(menu.findItem(R.id.action_search));
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.action_all:
                swapFragment(new SongListFragment());
                return true;
            case R.id.action_albums:
                swapFragment(new AlbumGridFragment());
                return true;
            case R.id.action_artists:
                swapFragment(new ArtistsFragment());
                return true;
            case R.id.action_playlist:
                swapFragment(new PlaylistsFragment());
                return true;
            case R.id.action_settings:
                Intent settingsActivity = new Intent(this, SettingsActivity.class);
                startActivityForResult(settingsActivity, REQUEST_SETTINGS);
                return true;
        }
        return super.onOptionsItemSelected(item);
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
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Load daily song only if it has permission
                loadDailySong();
                // TODO Placer ça dans un écran de présentation de l'application, au premier démarrage
                if (!Prefs.isDatabaseSetupComplete(this)) {
                    SetupService.startDatabaseSetup(this);
                }
            }
            // Whether it has permission or not, load fragment into interface
            if (!handleIntent(getIntent())) {
                mNavigationView.setCheckedItem(R.id.action_all);
                swapFragment(new SongListFragment());
            }
        }
    }

    /**
     * Replace the currently displayed fragment in the main container by the one specified.
     * Previous fragment will be destroyed, as the transaction is not put onto the backstack.
     * @param newFrag fragment to display in the main container
     */
    private void swapFragment(Fragment newFrag) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, newFrag)
                .commit();
    }

    /**
     * Prepare the Navigation Drawer header to display informations on the daily song.
     * Clicking on the header will start playing that song.
     * @param daily song to display as the Navigation Drawer header
     */
    private void prepareHeaderView(final MediaItem daily) {
        if (daily != null) {
            Uri artUri = daily.getDescription().getIconUri();
            CharSequence title = daily.getDescription().getTitle();
            CharSequence subtitle = daily.getDescription().getSubtitle();

            View header = getLayoutInflater().inflate(R.layout.drawer_header, mNavigationView, false);

            final View band = header.findViewById(R.id.band);
            ImageView albumArtView = (ImageView) header.findViewById(R.id.cover);

            final TextView titleText = ((TextView) header.findViewById(R.id.title));
            titleText.setText(title);

            final TextView subtitleText = ((TextView) header.findViewById(R.id.subtitle));
            subtitleText.setText(subtitle);

            // If header is already added, don't add it twice
            if (mNavigationView.getHeaderView(0) == null) {
                mNavigationView.addHeaderView(header);
            }

            final Drawable dummyAlbumArt = AppCompatResources.getDrawable(HomeActivity.this,
                    R.drawable.ic_audiotrack_24dp);

            Glide.with(this).fromUri().asBitmap()
                    .transcode(new BottomPaletteTranscoder(HomeActivity.this), PaletteBitmap.class)
                    .load(artUri)
                    .error(dummyAlbumArt)
                    .centerCrop()
                    .into(new ImageViewTarget<PaletteBitmap>(albumArtView) {
                        @Override
                        protected void setResource(PaletteBitmap resource) {
                            super.view.setImageBitmap(resource.bitmap);
                            Palette.Swatch swatch = resource.palette.getDominantSwatch();
                            if (swatch != null) {
                                band.setBackgroundColor(swatch.getRgb());
                                titleText.setTextColor(swatch.getBodyTextColor());
                                subtitleText.setTextColor(swatch.getBodyTextColor());
                            }
                        }
                    });

            header.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MediaControllerCompat controller = MediaControllerCompat
                            .getMediaController(HomeActivity.this);
                    if (controller != null) {
                        controller.getTransportControls().playFromMediaId(daily.getMediaId(), null);
                    }
                }
            });
        }
    }

    /**
     * Perform an action depending on the received intent.
     * This is intended to handle actions relative to launcher shortcuts (API 25+).
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
                    swapFragment(new AlbumGridFragment());
                    return true;
            }
        }
        return false;
    }

    /**
     * Start playing a random mix of all songs in the music library.
     * If MediaBrowser is not connected, playing will start asynchronously,
     * as soon as the MediaBrowser is connected.
     */
    private void startRandomMix() {
        MediaBrowserFragment mbf = MediaBrowserFragment.getInstance(getSupportFragmentManager());
        if (mbf.isConnected()) {
            mStartRandomMix.onConnected();
        } else mbf.doWhenConnected(mStartRandomMix);
    }
}
