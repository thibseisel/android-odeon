package fr.nihilus.music.ui.albums;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.res.ColorStateList;
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
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import fr.nihilus.music.R;
import fr.nihilus.music.library.BrowserViewModel;
import fr.nihilus.music.library.ViewModelFactory;
import fr.nihilus.music.utils.MediaID;
import fr.nihilus.music.utils.ViewUtils;
import fr.nihilus.music.view.CurrentlyPlayingDecoration;

public class AlbumDetailActivity extends AppCompatActivity
        implements View.OnClickListener, TrackAdapter.OnTrackSelectedListener {

    public static final String ALBUM_ART_TRANSITION_NAME = "albumArt";
    public static final String ARG_PALETTE = "palette";
    public static final String ARG_PICKED_ALBUM = "pickedAlbum";

    private static final String TAG = "AlbumDetailActivity";
    private static final String KEY_ITEMS = "tracks";
    @Inject ViewModelFactory mFactory;
    private TrackAdapter mAdapter;
    private MediaItem mPickedAlbum;
    private CollapsingToolbarLayout mCollapsingToolbar;
    private TextView mAlbumTitle;
    private TextView mAlbumArtist;
    private RecyclerView mRecyclerView;
    private final SubscriptionCallback mSubscriptionCallback = new SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaItem> children) {
            Log.d(TAG, "onChildrenLoaded: loaded " + children.size() + " tracks");
            mAdapter.updateTracks(children);
            mRecyclerView.swapAdapter(mAdapter, false);
        }
    };
    private FloatingActionButton mPlayFab;
    private CurrentlyPlayingDecoration mDecoration;
    private BrowserViewModel mViewModel;

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

        mAlbumTitle = findViewById(R.id.title);
        mAlbumTitle.setText(mPickedAlbum.getDescription().getTitle());
        mAlbumArtist = findViewById(R.id.subtitle);
        mAlbumArtist.setText(mPickedAlbum.getDescription().getSubtitle());

        mPlayFab = findViewById(R.id.action_play);
        mPlayFab.setOnClickListener(this);

        setupToolbar();
        setupAlbumArt();
        setupTrackList();
        applyPaletteTheme(callingActivity.getIntArrayExtra(ARG_PALETTE));

        mViewModel = ViewModelProviders.of(this, mFactory).get(BrowserViewModel.class);
        mViewModel.connect();
        mViewModel.getCurrentMetadata().observe(this, new Observer<MediaMetadataCompat>() {
            @Override
            public void onChanged(@Nullable MediaMetadataCompat metadata) {
                decoratePlayingTrack(metadata);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mViewModel.subscribe(mPickedAlbum.getMediaId(), mSubscriptionCallback);
    }

    @Override
    protected void onStop() {
        mViewModel.unsubscribe(mPickedAlbum.getMediaId());
        super.onStop();
    }

    private void setupAlbumArt() {
        ImageView albumArtView = findViewById(R.id.cover);
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
        mRecyclerView = findViewById(android.R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mDecoration = new CurrentlyPlayingDecoration(this);
        mRecyclerView.addItemDecoration(mDecoration);

        mAdapter = new TrackAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setOnTrackSelectedListener(this);
    }

    private void setupToolbar() {
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        mCollapsingToolbar = findViewById(R.id.collapsingToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(null);
    }

    /**
     * Apply colors picked from the album art on the user interface.
     *
     * @param colors array of colors containing the following :
     *               <ul>
     *               <li>[0] Primary Color</li>
     *               <li>[1] Accent Color</li>
     *               <li>[2] Title text color</li>
     *               <li>[3] Body text color</li>
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
        mPlayFab.setBackgroundTintList(ColorStateList.valueOf(colors[1]));
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
        mViewModel.playFromMediaId(item.getMediaId());
    }

    /**
     * Adds an {@link CurrentlyPlayingDecoration} to a track of this album if it's currently playing.
     *
     * @param playingTrack the currently playing track
     */
    private void decoratePlayingTrack(@Nullable MediaMetadataCompat playingTrack) {
        if (playingTrack != null) {
            String musicId = playingTrack.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
            Log.d(TAG, "onMetadataChanged: musicId is " + musicId);
            for (int i = 0; i < mAdapter.getItemCount(); i++) {
                final String trackMediaId = mAdapter.get(i).getMediaId();
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
