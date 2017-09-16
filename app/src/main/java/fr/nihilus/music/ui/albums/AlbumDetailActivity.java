package fr.nihilus.music.ui.albums;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaControllerCompat.Callback;
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

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import fr.nihilus.music.R;
import fr.nihilus.music.library.MediaBrowserConnection;
import fr.nihilus.music.utils.MediaID;
import fr.nihilus.music.utils.ViewUtils;
import fr.nihilus.music.view.CurrentlyPlayingDecoration;
import io.reactivex.functions.Consumer;

public class AlbumDetailActivity extends AppCompatActivity
        implements View.OnClickListener, TrackAdapter.OnTrackSelectedListener {

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

    @Inject MediaBrowserConnection mBrowserConnection;

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

    private final Callback mControllerCallback = new MediaControllerCompat.Callback() {

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            // FIXME Display of playing track does not update correctly
            decoratePlayingTrack(metadata);
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_detail);

        final Intent callingActivity = getIntent();
        mPickedAlbum = callingActivity.getParcelableExtra(ARG_PICKED_ALBUM);
        if (mPickedAlbum == null) {
            throw new IllegalStateException("Calling activity must specify the album to display.");
        }

        if (savedInstanceState != null) {
            mTracks = savedInstanceState.getParcelableArrayList(KEY_ITEMS);
        } else mTracks = new ArrayList<>();

        mAlbumTitle = (TextView) findViewById(R.id.title);
        mAlbumTitle.setText(mPickedAlbum.getDescription().getTitle());
        mAlbumArtist = (TextView) findViewById(R.id.subtitle);
        mAlbumArtist.setText(mPickedAlbum.getDescription().getSubtitle());

        mFab = (FloatingActionButton) findViewById(R.id.action_play);
        mFab.setOnClickListener(this);

        setupToolbar();
        setupAlbumArt();
        setupTrackList();
        applyPaletteTheme(callingActivity.getIntArrayExtra(ARG_PALETTE));

        mBrowserConnection.connect();
        mBrowserConnection.getMediaController().subscribe(new Consumer<MediaControllerCompat>() {
            @Override
            public void accept(@Nullable MediaControllerCompat controller) throws Exception {
                MediaControllerCompat.setMediaController(AlbumDetailActivity.this, controller);
                if (controller != null) {
                    Log.d(TAG, "onConnected: register controller callback.");
                    controller.registerCallback(mControllerCallback);
                    decoratePlayingTrack(controller.getMetadata());
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mBrowserConnection.subscribe(mPickedAlbum.getMediaId(), mSubscriptionCallback);
    }

    @Override
    protected void onStop() {
        mBrowserConnection.unsubscribe(mPickedAlbum.getMediaId());
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mBrowserConnection.getMediaController().subscribe(new Consumer<MediaControllerCompat>() {
            @Override
            public void accept(@Nullable MediaControllerCompat controller) throws Exception {
                if (controller != null) {
                    controller.unregisterCallback(mControllerCallback);
                }
            }
        });
        mBrowserConnection.release();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(KEY_ITEMS, mTracks);
    }

    private void setupAlbumArt() {
        ImageView albumArtView = (ImageView) findViewById(R.id.cover);
        ViewCompat.setTransitionName(albumArtView, ALBUM_ART_TRANSITION_NAME);
        try {
            Bitmap albumArt = Media.getBitmap(getContentResolver(),
                    mPickedAlbum.getDescription().getIconUri());
            albumArtView.setImageBitmap(albumArt);
        } catch (IOException e) {
            Log.e(TAG, "onCreate: Failed to load bitmap.", e);
        }
    }

    private void setupTrackList() {
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
        getSupportActionBar().setTitle(null);
    }

    /**
     * Apply colors picked from the album art on the user interface.
     * @param colors array of colors containing the following :
     *               <ul>
     *                   <li>[0] Primary Color</li>
     *                   <li>[1] Accent Color</li>
     *                   <li>[2] Title text color</li>
     *                   <li>[3] Body text color</li>
     *               </ul>
     */
    private void applyPaletteTheme(@ColorInt int[] colors) {
        @ColorInt int statusBarColor = ViewUtils.darker(colors[0], 0.8f);
        mCollapsingToolbar.setStatusBarScrimColor(statusBarColor);
        mCollapsingToolbar.setContentScrimColor(colors[0]);
        findViewById(R.id.band).setBackgroundColor(colors[0]);
        mAlbumTitle.setTextColor(colors[2]);
        mAlbumArtist.setTextColor(colors[3]);
        mDecoration.setIconColor(colors[1]);
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

    /**
     * Adds an {@link CurrentlyPlayingDecoration} to a track of this album if it's currently playing.
     * @param playingTrack the currently playing track
     */
    private void decoratePlayingTrack(@Nullable MediaMetadataCompat playingTrack) {
        if (playingTrack != null) {
            String musicId = playingTrack.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
            Log.d(TAG, "onMetadataChanged: musicId is " + musicId);
            for (int i = 0; i < mTracks.size(); i++) {
                final String trackMediaId = mTracks.get(i).getMediaId();
                Log.d(TAG, "onMetadataChanged: compating with " + trackMediaId);
                if (MediaID.extractMusicID(trackMediaId).equals(musicId)) {
                    mDecoration.setDecoratedItemPosition(i);
                    mRecyclerView.invalidateItemDecorations();
                    return;
                }
            }
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
