/*
 * Copyright 2019 Thibault Seisel
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

package fr.nihilus.music.library.albums

import android.arch.lifecycle.ViewModelProviders
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaMetadataCompat
import android.support.v7.view.ContextThemeWrapper
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.ImageView
import fr.nihilus.music.R
import fr.nihilus.music.base.BaseActivity
import fr.nihilus.music.extensions.darkSystemIcons
import fr.nihilus.music.extensions.luminance
import fr.nihilus.music.extensions.observeK
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.ui.CurrentlyPlayingDecoration
import fr.nihilus.music.ui.LoadRequest
import kotlinx.android.synthetic.main.activity_album_detail.*
import javax.inject.Inject

class AlbumDetailActivity : BaseActivity(),
    View.OnClickListener,
    BaseAdapter.OnItemSelectedListener {

    @Inject lateinit var albumPalette: AlbumPalette
    @Inject lateinit var pickedAlbum: MediaItem

    private lateinit var adapter: TrackAdapter
    private lateinit var decoration: CurrentlyPlayingDecoration

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(this, viewModelFactory)[AlbumDetailViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album_detail)

        viewModel.loadTracksOfAlbum(pickedAlbum)

        with(pickedAlbum.description) {
            title_view.text = title
            subtitle_view.text = subtitle
        }

        play_fab.setOnClickListener(this)

        setupToolbar()
        setupAlbumArt()
        setupTrackList()

        applyPaletteTheme(albumPalette)

        // Change the decorated item when metadata changes
        viewModel.nowPlaying.observeK(this, this::decoratePlayingTrack)

        // Subscribe to children of this album
        viewModel.children.observeK(this) { tracksUpdateRequest ->
            when (tracksUpdateRequest) {
                is LoadRequest.Success -> {
                    adapter.submitList(tracksUpdateRequest.data)
                    val currentMetadata = viewModel.nowPlaying.value
                    decoratePlayingTrack(currentMetadata)
                }
            }
        }
    }

    private fun setupAlbumArt() {
        val albumArtView: ImageView = findViewById(R.id.album_art_view)
        albumArtView.transitionName =
                ALBUM_ART_TRANSITION_NAME

        GlideApp.with(this).asBitmap()
            .load(pickedAlbum.description.iconUri)
            .fallback(R.drawable.ic_album_24dp)
            .centerCrop()
            .into(albumArtView)
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

        with(supportActionBar!!) {
            setDisplayHomeAsUpEnabled(false)
            title = null
        }
    }

    /**
     * Apply colors picked from the album art to the user interface.
     */
    private fun applyPaletteTheme(palette: AlbumPalette) {
        album_info_layout.setBackgroundColor(palette.primary)
        title_view.setTextColor(palette.titleText)
        subtitle_view.setTextColor(palette.bodyText)

        val darkStatusText = palette.bodyText.luminance < 0.5f
        setDarkHomeUpIndicator(darkStatusText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.darkSystemIcons = darkStatusText
        }

        collapsing_toolbar.setStatusBarScrimColor(palette.primaryDark)
        collapsing_toolbar.setContentScrimColor(palette.primary)

        play_fab.backgroundTintList = ColorStateList.valueOf(palette.accent)
        play_fab.imageTintList = ColorStateList.valueOf(palette.textOnAccent)
        decoration = CurrentlyPlayingDecoration(this, palette.accent)
        recycler.addItemDecoration(decoration)
    }

    override fun onClick(view: View) {
        if (R.id.play_fab == view.id) {
            playMediaItem(pickedAlbum)
        }
    }

    override fun onItemSelected(position: Int, actionId: Int) {
        val selectedTrack = adapter.getItem(position)
        playMediaItem(selectedTrack)
    }

    private fun playMediaItem(item: MediaItem) {
        viewModel.playMedia(item)
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
                decoration.decoratedPosition = position
                recycler.invalidateItemDecorations()
            }
        }
    }

    private fun setDarkHomeUpIndicator(dark: Boolean) {
        val themedContext = ContextThemeWrapper(this,
            if (dark) R.style.ThemeOverlay_AppCompat_Light
            else R.style.ThemeOverlay_AppCompat_Dark_ActionBar
        )

        val upArrow = ContextCompat.getDrawable(themedContext, R.drawable.ic_arrow_back_24dp)
        with(supportActionBar!!) {
            setHomeAsUpIndicator(upArrow)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    companion object {
        const val ALBUM_ART_TRANSITION_NAME = "albumArt"
        const val ARG_PALETTE = "palette"
        const val ARG_PICKED_ALBUM = "pickedAlbum"
    }
}
