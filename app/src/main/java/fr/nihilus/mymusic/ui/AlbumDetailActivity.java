package fr.nihilus.mymusic.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fr.nihilus.mymusic.MediaBrowserFragment;
import fr.nihilus.mymusic.R;
import fr.nihilus.mymusic.utils.ViewUtils;

import static fr.nihilus.mymusic.R.id.albumArt;

@SuppressWarnings("ConstantConditions")
public class AlbumDetailActivity extends AppCompatActivity
        implements View.OnClickListener, TrackAdapter.OnTrackSelectedListener {

    public static final String ALBUM_ART_TRANSITION_NAME = "albumArt";
    public static final String ARG_PALETTE = "palette";
    public static final String ARG_PICKED_ALBUM = "pickedAlbum";

    private static final String TAG = "AlbumDetailActivity";
    private static final String KEY_ITEMS = "tracks";

    private TrackAdapter mAdapter;
    private ArrayList<MediaItem> mTracks;
    private final SubscriptionCallback mSubscriptionCallback = new SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, List<MediaItem> children) {
            mTracks.clear();
            mTracks.addAll(children);
            mAdapter.notifyDataSetChanged();
        }
    };
    private MediaItem mPickedAlbum;
    private CollapsingToolbarLayout mCollapsingToolbar;
    private FloatingActionButton mFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_detail);

        final Intent callingActivity = getIntent();
        mPickedAlbum = callingActivity.getParcelableExtra(ARG_PICKED_ALBUM);
        if (mPickedAlbum == null) {
            throw new IllegalStateException("Calling activity must specify the album to display.");
        }

        mFab = (FloatingActionButton) findViewById(R.id.action_play);
        mFab.setOnClickListener(this);

        setupToolbar();
        @ColorInt int[] themeColors = callingActivity.getIntArrayExtra(ARG_PALETTE);
        applyPaletteTheme(themeColors);

        ImageView albumArtView = (ImageView) findViewById(albumArt);
        ViewCompat.setTransitionName(albumArtView, ALBUM_ART_TRANSITION_NAME);
        try {
            Bitmap albumArt = Media.getBitmap(getContentResolver(),
                    mPickedAlbum.getDescription().getIconUri());
            albumArtView.setImageBitmap(albumArt);
        } catch (IOException e) {
            Log.e(TAG, "onCreate: Failed to load bitmap.", e);
        }

        if (savedInstanceState != null) {
            mTracks = savedInstanceState.getParcelableArrayList(KEY_ITEMS);
        } else {
            mTracks = new ArrayList<>();
            MediaBrowserFragment.getInstance(getSupportFragmentManager())
                    .subscribe(mPickedAlbum.getMediaId(), mSubscriptionCallback);
        }

        RecyclerView recyclerView = (RecyclerView) findViewById(android.R.id.list);
        mAdapter = new TrackAdapter(mTracks);
        recyclerView.setAdapter(mAdapter);
        mAdapter.setOnTrackSelectedListener(this);
    }

    private void setupToolbar() {
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        mCollapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsingToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(mPickedAlbum.getDescription().getTitle());
    }

    private void applyPaletteTheme(@ColorInt int[] colors) {
        mCollapsingToolbar.setStatusBarScrimColor(colors[0]);
        mCollapsingToolbar.setContentScrimColor(colors[0]);
        mCollapsingToolbar.setExpandedTitleColor(colors[2]);
        // TODO Changer couleur FAB avec colors[1]
        if (ViewUtils.isColorBright(colors[0])) {
            Log.d(TAG, "applyPaletteTheme: color is bright.");
            ViewUtils.setLightStatusBar(mCollapsingToolbar, true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        MediaBrowserFragment.getInstance(getSupportFragmentManager())
                .unsubscribe(mPickedAlbum.getMediaId());
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        mCollapsingToolbar.setTitle(title);
    }

    @Override
    public void onClick(View view) {
        if (R.id.action_play == view.getId()) {
            // Play whole album
            Toast.makeText(this, "Playing album with ID " + mPickedAlbum.getMediaId(),
                    Toast.LENGTH_SHORT).show();
            //playMediaItem(mPickedAlbum);
        }
    }

    private void playMediaItem(MediaItem item) {
        MediaControllerCompat controller = getSupportMediaController();
        if (controller != null && item.isPlayable()) {
            controller.getTransportControls().playFromMediaId(item.getMediaId(), null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public void onTrackSelected(MediaItem track) {
        playMediaItem(track);
    }
}
