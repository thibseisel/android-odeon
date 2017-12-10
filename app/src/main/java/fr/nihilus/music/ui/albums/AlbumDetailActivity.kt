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

package fr.nihilus.music.ui.albums

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.res.ColorStateList
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.design.widget.CollapsingToolbarLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.view.ViewCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import dagger.android.AndroidInjection
import fr.nihilus.music.R
import fr.nihilus.music.client.BrowserViewModel
import fr.nihilus.music.client.ViewModelFactory
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.utils.MediaID
import fr.nihilus.music.utils.darker
import fr.nihilus.music.view.CurrentlyPlayingDecoration
import javax.inject.Inject

class AlbumDetailActivity : AppCompatActivity(),
        View.OnClickListener,
        TrackAdapter.OnTrackSelectedListener {

    private lateinit var mAdapter: TrackAdapter
    private lateinit var mPickedAlbum: MediaItem

    private lateinit var mCollapsingToolbar: CollapsingToolbarLayout
    private lateinit var mAlbumTitle: TextView
    private lateinit var mAlbumArtist: TextView
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mPlayFab: FloatingActionButton
    private lateinit var mDecoration: CurrentlyPlayingDecoration

    @Inject lateinit var mFactory: ViewModelFactory
    private lateinit var mViewModel: BrowserViewModel

    private val mSubscriptionCallback = object : SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: List<MediaItem>) {
            mAdapter.updateTracks(children)
            mRecyclerView.swapAdapter(mAdapter, false)

            val currentMetadata = mViewModel.currentMetadata.value
            decoratePlayingTrack(currentMetadata)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album_detail)

        mPickedAlbum = checkNotNull(intent.getParcelableExtra(ARG_PICKED_ALBUM)) {
            "Calling activity must specify the album to display."
        }

        mAlbumTitle = findViewById(R.id.title)
        mAlbumTitle.text = mPickedAlbum.description.title
        mAlbumArtist = findViewById(R.id.subtitle)
        mAlbumArtist.text = mPickedAlbum.description.subtitle

        mPlayFab = findViewById(R.id.action_play)
        mPlayFab.setOnClickListener(this)

        setupToolbar()
        setupAlbumArt()
        setupTrackList()
        applyPaletteTheme(intent.getIntArrayExtra(ARG_PALETTE))

        mViewModel = ViewModelProviders.of(this, mFactory).get(BrowserViewModel::class.java)
        mViewModel.connect()
        mViewModel.currentMetadata.observe(this, Observer(this::decoratePlayingTrack))
    }

    override fun onStart() {
        super.onStart()
        mViewModel.subscribe(mPickedAlbum.mediaId!!, mSubscriptionCallback)
    }

    override fun onStop() {
        mViewModel.unsubscribe(mPickedAlbum.mediaId!!)
        super.onStop()
    }

    private fun setupAlbumArt() {
        val albumArtView = findViewById<ImageView>(R.id.cover)
        ViewCompat.setTransitionName(albumArtView, ALBUM_ART_TRANSITION_NAME)
        GlideApp.with(this).load(mPickedAlbum.description.iconUri).into(albumArtView)
    }

    private fun setupTrackList() {
        mRecyclerView = findViewById(android.R.id.list)
        mRecyclerView.layoutManager = LinearLayoutManager(this)

        mAdapter = TrackAdapter()
        mRecyclerView.adapter = mAdapter
        mAdapter.setOnTrackSelectedListener(this)
    }

    private fun setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar))
        mCollapsingToolbar = findViewById(R.id.collapsingToolbar)
        supportActionBar!!.apply {
            setDisplayHomeAsUpEnabled(true)
            title = null
        }
    }

    /**
     * Apply colors picked from the album art on the user interface.
     *
     * @param colors array of colors containing the following :
     *
     *  * [0] Primary Color
     *  * [1] Accent Color
     *  * [2] Title text color
     *  * [3] Body text color
     *
     */
    private fun applyPaletteTheme(@ColorInt colors: IntArray) {
        val statusBarColor = darker(colors[0], 0.8f)
        mCollapsingToolbar.setStatusBarScrimColor(statusBarColor)
        mCollapsingToolbar.setContentScrimColor(colors[0])
        findViewById<View>(R.id.band).setBackgroundColor(colors[0])
        mAlbumTitle.setTextColor(colors[2])
        mAlbumArtist.setTextColor(colors[3])
        mPlayFab.backgroundTintList = ColorStateList.valueOf(colors[1])

        mDecoration = CurrentlyPlayingDecoration(this, colors[1])
        mRecyclerView.addItemDecoration(mDecoration)

        /*if (ViewUtils.isColorBright(statusBarColor)) {
            ViewUtils.setLightStatusBar(mCollapsingToolbar, true);
        }*/
    }

    override fun onClick(view: View) {
        if (R.id.action_play == view.id) {
            playMediaItem(mPickedAlbum)
        }
    }

    private fun playMediaItem(item: MediaItem) {
        mViewModel.post {
            it.transportControls.playFromMediaId(item.mediaId, null)
        }
    }

    /**
     * Adds an [CurrentlyPlayingDecoration] to a track of this album if it's currently playing.
     *
     * @param playingTrack the currently playing track
     */
    private fun decoratePlayingTrack(playingTrack: MediaMetadataCompat?) {
        if (playingTrack != null) {
            val musicId = playingTrack.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
            val position = mAdapter.items.indexOfFirst {
                musicId == MediaID.extractMusicID(it.mediaId)
            }

            if (position != -1) {
                mDecoration.setDecoratedItemPosition(position)
                mRecyclerView.invalidateItemDecorations()
            }
        }
    }

    override fun onTrackSelected(track: MediaItem) {
        playMediaItem(track)
    }

    companion object {
        const val ALBUM_ART_TRANSITION_NAME = "albumArt"
        const val ARG_PALETTE = "palette"
        const val ARG_PICKED_ALBUM = "pickedAlbum"
    }
}
