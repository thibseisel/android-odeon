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
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.view.ViewCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.ImageView
import dagger.android.AndroidInjection
import fr.nihilus.music.R
import fr.nihilus.music.client.BrowserViewModel
import fr.nihilus.music.client.ViewModelFactory
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.utils.darker
import fr.nihilus.music.view.CurrentlyPlayingDecoration
import kotlinx.android.synthetic.main.activity_album_detail.*
import javax.inject.Inject

class AlbumDetailActivity : AppCompatActivity(),
        View.OnClickListener,
        BaseAdapter.OnItemSelectedListener {

    @Inject lateinit var mFactory: ViewModelFactory

    private lateinit var adapter: TrackAdapter
    private lateinit var pickedAlbum: MediaItem
    private lateinit var decoration: CurrentlyPlayingDecoration
    private lateinit var mViewModel: BrowserViewModel

    private val subscriptionCallback = object : SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: List<MediaItem>) {
            adapter.update(children)
            recycler.swapAdapter(adapter, false)

            val currentMetadata = mViewModel.currentMetadata.value
            decoratePlayingTrack(currentMetadata)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album_detail)

        pickedAlbum = checkNotNull(intent.getParcelableExtra(ARG_PICKED_ALBUM)) {
            "Calling activity must specify the album to display."
        }

        with(pickedAlbum.description) {
            titleView.text = title
            subtitleView.text = subtitle
        }

        playFab.setOnClickListener(this)

        setupToolbar()
        setupAlbumArt()
        setupTrackList()
        applyPaletteTheme(intent.getIntArrayExtra(ARG_PALETTE))

        mViewModel = ViewModelProviders.of(this, mFactory).get(BrowserViewModel::class.java)
        mViewModel.connect()

        // Change the decorated item when metadata changes
        mViewModel.currentMetadata.observe(this, Observer(this::decoratePlayingTrack))
    }

    override fun onStart() {
        super.onStart()
        mViewModel.subscribe(pickedAlbum.mediaId!!, subscriptionCallback)
    }

    override fun onStop() {
        mViewModel.unsubscribe(pickedAlbum.mediaId!!)
        super.onStop()
    }

    private fun setupAlbumArt() {
        val albumArtView: ImageView = findViewById(R.id.albumArtView)
        ViewCompat.setTransitionName(albumArtView, ALBUM_ART_TRANSITION_NAME)
        GlideApp.with(this).load(pickedAlbum.description.iconUri).into(albumArtView)
    }

    private fun setupTrackList() {
        recycler.let {
            it.layoutManager = LinearLayoutManager(this)
            adapter = TrackAdapter(this)
            it.adapter = adapter
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)

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
        collapsingToolbar.setStatusBarScrimColor(statusBarColor)
        collapsingToolbar.setContentScrimColor(colors[0])
        findViewById<View>(R.id.albumInfoLayout).setBackgroundColor(colors[0])
        titleView.setTextColor(colors[2])
        subtitleView.setTextColor(colors[3])
        playFab.backgroundTintList = ColorStateList.valueOf(colors[1])

        decoration = CurrentlyPlayingDecoration(this, colors[1])
        recycler.addItemDecoration(decoration)
    }

    override fun onClick(view: View) {
        if (R.id.playFab == view.id) {
            playMediaItem(pickedAlbum)
        }
    }

    override fun onItemSelected(position: Int, actionId: Int) {
        val selectedTrack = adapter[position]
        playMediaItem(selectedTrack)
    }

    private fun playMediaItem(item: MediaItem) {
        mViewModel.post {
            it.transportControls.playFromMediaId(item.mediaId, null)
        }
    }

    /**
     * Adds an [CurrentlyPlayingDecoration] to a track of this album if it is currently playing.
     *
     * @param playingTrack the currently playing track.
     */
    private fun decoratePlayingTrack(playingTrack: MediaMetadataCompat?) {
        if (playingTrack != null) {
            val position = adapter.indexOf(playingTrack)

            if (position != -1) {
                decoration.setDecoratedItemPosition(position)
                recycler.invalidateItemDecorations()
            }
        }
    }

    companion object {
        const val ALBUM_ART_TRANSITION_NAME = "albumArt"
        const val ARG_PALETTE = "palette"
        const val ARG_PICKED_ALBUM = "pickedAlbum"
    }
}
