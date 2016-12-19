package fr.nihilus.mymusic.ui.albums;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.os.Build;
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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fr.nihilus.mymusic.MediaBrowserFragment;
import fr.nihilus.mymusic.R;
import fr.nihilus.mymusic.utils.ViewUtils;
import fr.nihilus.mymusic.view.CurrentlyPlayingDecoration;

@SuppressWarnings("ConstantConditions")
public class AlbumDetailActivity extends AppCompatActivity
        implements View.OnClickListener, TrackAdapter.OnTrackSelectedListener {

    private static final int LEVEL_PLAYING = 1;
    private static final int LEVEL_PAUSED = 0;

    private final SubscriptionCallback mSubscriptionCallback = new SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, List<MediaItem> children) {
            Log.d(TAG, "onChildrenLoaded: loaded " + children.size() + " tracks");
            mTracks.clear();
            mTracks.addAll(children);
            mAdapter.updateTracks(children);
            mRecyclerView.swapAdapter(mAdapter, false);
        }
    };

    public static final String ALBUM_ART_TRANSITION_NAME = "albumArt";
    public static final String ARG_PALETTE = "palette";
    public static final String ARG_PICKED_ALBUM = "pickedAlbum";

    private static final String TAG = "AlbumDetailActivity";
    private static final String KEY_ITEMS = "tracks";

    private TrackAdapter mAdapter;
    private ArrayList<MediaItem> mTracks;

    private MediaItem mPickedAlbum;
    private CollapsingToolbarLayout mCollapsingToolbar;
    private FloatingActionButton mFab;
    private TextView mAlbumTitle;
    private TextView mAlbumArtist;
    private RecyclerView mRecyclerView;
    private CurrentlyPlayingDecoration mDecoration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_detail);

        final Intent callingActivity = getIntent();
        mPickedAlbum = callingActivity.getParcelableExtra(ARG_PICKED_ALBUM);
        if (mPickedAlbum == null) {
            throw new IllegalStateException("Calling activity must specify the album to display.");
        }

        // TODO Placer le bandeau dans le RecyclerView
        mAlbumTitle = (TextView) findViewById(R.id.title);
        mAlbumTitle.setText(mPickedAlbum.getDescription().getTitle());
        mAlbumArtist = (TextView) findViewById(R.id.subtitle);
        mAlbumArtist.setText(mPickedAlbum.getDescription().getSubtitle());

        mFab = (FloatingActionButton) findViewById(R.id.action_play);
        mFab.setOnClickListener(this);

        setupToolbar();
        @ColorInt int[] themeColors = callingActivity.getIntArrayExtra(ARG_PALETTE);
        applyPaletteTheme(themeColors);

        ImageView albumArtView = (ImageView) findViewById(R.id.albumArt);
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

        mRecyclerView = (RecyclerView) findViewById(android.R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ViewCompat.setNestedScrollingEnabled(mRecyclerView, false);
        mDecoration = new CurrentlyPlayingDecoration(this);
        mRecyclerView.addItemDecoration(mDecoration);

        mAdapter = new TrackAdapter(mTracks);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setOnTrackSelectedListener(this);
    }

    private void setupToolbar() {
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        mCollapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsingToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void applyPaletteTheme(@ColorInt int[] colors) {
        @ColorInt int statusBarColor = ViewUtils.darker(colors[0], 0.8f);
        mCollapsingToolbar.setStatusBarScrimColor(statusBarColor);
        findViewById(R.id.band).setBackgroundColor(colors[0]);
        mAlbumTitle.setTextColor(colors[2]);
        mAlbumArtist.setTextColor(colors[3]);
        if (ViewUtils.isColorBright(statusBarColor)) {
            ViewUtils.setLightStatusBar(mCollapsingToolbar, true);
        }
    }

    @Override
    public void onClick(View view) {
        if (R.id.action_play == view.getId()) {
            playMediaItem(mPickedAlbum);
        }
    }

    private void playMediaItem(MediaItem item) {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(this);
        if (controller != null && item.isPlayable()) {
            controller.getTransportControls().playFromMediaId(item.getMediaId(), null);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(KEY_ITEMS, mTracks);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public void onTrackSelected(int position, MediaItem track) {
        mDecoration.setDecoratedItemPosition(position);
        mRecyclerView.invalidateItemDecorations();
        playMediaItem(track);
    }

    private void togglePlayPauseButton(boolean isPlaying) {
        mFab.setImageLevel(isPlaying ? LEVEL_PLAYING : LEVEL_PAUSED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Animatable avd = (Animatable) mFab.getDrawable().getCurrent();
            avd.start();
        }
    }
}
