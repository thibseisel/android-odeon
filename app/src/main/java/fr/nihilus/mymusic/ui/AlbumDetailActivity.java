package fr.nihilus.mymusic.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
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

public class AlbumDetailActivity extends AppCompatActivity
        implements View.OnClickListener, TrackAdapter.OnTrackSelectedListener {

    private static final String TAG = "AlbumDetailActivity";

    public static final String ARG_PICKED_ALBUM = "pickedAlbum";
    private static final String KEY_ITEMS = "tracks";

    private TrackAdapter mAdapter;
    private ArrayList<MediaItem> mTracks;
    private MediaItem mPickedAlbum;

    private final SubscriptionCallback mSubscriptionCallback = new SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, List<MediaItem> children) {
            mTracks.clear();
            mTracks.addAll(children);
            mAdapter.notifyDataSetChanged();
        }
    };
    private CollapsingToolbarLayout mCollapsingToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_detail);

        final Intent callingActivity = getIntent();
        mPickedAlbum = callingActivity.getParcelableExtra(ARG_PICKED_ALBUM);
        if (mPickedAlbum == null) {
            throw new IllegalStateException("Calling activity must specify the album to display.");
        }

        setupToolbar();

        ImageView albumArtView = (ImageView) findViewById(R.id.albumArt);
        ViewCompat.setTransitionName(albumArtView, "albumArt");
        try {
            Bitmap albumArt = Media.getBitmap(getContentResolver(),
                    mPickedAlbum.getDescription().getIconUri());
            albumArtView.setImageBitmap(albumArt);
            applyPaletteTheme(albumArt);
        } catch (IOException e) {
            Log.e(TAG, "onCreate: Failed to load bitmap.", e);
        }

        RecyclerView recyclerView = (RecyclerView) findViewById(android.R.id.list);

        if (savedInstanceState != null) {
            mTracks = savedInstanceState.getParcelableArrayList(KEY_ITEMS);
        } else {
            mTracks = new ArrayList<>();
            MediaBrowserFragment.getInstance(getSupportFragmentManager())
                    .subscribe(mPickedAlbum.getMediaId(), mSubscriptionCallback);
        }

        mAdapter = new TrackAdapter(this, mTracks);
        recyclerView.setAdapter(mAdapter);
        mAdapter.setOnTrackSelectedListener(this);

        findViewById(R.id.action_play).setOnClickListener(this);
    }

    private void setupToolbar() {
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        mCollapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsingToolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(mPickedAlbum.getDescription().getTitle());
    }

    private void applyPaletteTheme(Bitmap albumArt) {
        int titleColor = 0xff000000;
        Palette palette = Palette.from(albumArt).generate();
        Palette.Swatch swatch = palette.getDominantSwatch();
        if (swatch != null) {
            titleColor = swatch.getTitleTextColor();
        }
        mCollapsingToolbar.setExpandedTitleColor(titleColor);
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
