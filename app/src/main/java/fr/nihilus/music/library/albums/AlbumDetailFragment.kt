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

import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import fr.nihilus.music.R
import fr.nihilus.music.base.BaseFragment
import fr.nihilus.music.extensions.darkSystemIcons
import fr.nihilus.music.extensions.luminance
import fr.nihilus.music.extensions.observeK
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.ui.CurrentlyPlayingDecoration
import fr.nihilus.music.ui.LoadRequest
import kotlinx.android.synthetic.main.fragment_album_detail.*

class AlbumDetailFragment : BaseFragment(), BaseAdapter.OnItemSelectedListener {

    private lateinit var adapter: TrackAdapter
    private lateinit var decoration: CurrentlyPlayingDecoration

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(this, viewModelFactory)[AlbumDetailViewModel::class.java]
    }

    private val pickedAlbum: MediaBrowserCompat.MediaItem by lazy(LazyThreadSafetyMode.NONE) {
        arguments?.getParcelable<MediaBrowserCompat.MediaItem>(ARG_ALBUM)
            ?: error("Fragment should be created with its newInstance function.")
    }

    private val albumPalette: AlbumPalette by lazy(LazyThreadSafetyMode.NONE) {
        arguments?.getParcelable<AlbumPalette>(ARG_PALETTE)
            ?: error("Fragment should be created with its newInstance function.")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_album_detail, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        requireActivity().startPostponedEnterTransition()
        viewModel.loadTracksOfAlbum(pickedAlbum)

        with(pickedAlbum.description) {
            title_view.text = title
            subtitle_view.text = subtitle
        }

        play_fab.setOnClickListener {
            viewModel.playMedia(pickedAlbum)
        }

        setupToolbar()
        setupAlbumArt()
        setupTrackList()

        applyPaletteTheme(albumPalette)

        // Change the decorated item when metadata changes
        viewModel.nowPlaying.observeK(this, this::decoratePlayingTrack)

        // Subscribe to children of this album
        viewModel.children.observeK(this) { trackUpdateRequest ->
            if (trackUpdateRequest is LoadRequest.Success) {
                adapter.submitList(trackUpdateRequest.data)
                val currentMetadata = viewModel.nowPlaying.value
                decoratePlayingTrack(currentMetadata)
            }
        }
    }

    override fun onItemSelected(position: Int, actionId: Int) {
        val selectedTrack = adapter.getItem(position)
        viewModel.playMedia(selectedTrack)
    }

    private fun decoratePlayingTrack(playingTrack: MediaMetadataCompat?) {
        if (playingTrack != null) {
            val position = adapter.indexOf(playingTrack)

            if (position != -1) {
                decoration.decoratedPosition = position
                recycler.invalidateItemDecorations()
            }
        }
    }

    private fun setupToolbar() = with(requireActivity() as AppCompatActivity) {
        setSupportActionBar(toolbar)

        supportActionBar!!.run {
            setDisplayHomeAsUpEnabled(false)
            title = null
        }
    }

    private fun setupAlbumArt() {
        val albumArtView: ImageView = album_art_view
        albumArtView.transitionName = ALBUM_ART_TRANSITION_NAME

        GlideApp.with(this).asBitmap()
            .load(pickedAlbum.description.iconUri)
            .fallback(R.drawable.ic_album_24dp)
            .centerCrop()
            .into(albumArtView)
    }

    private fun setupTrackList() {
        val context = requireContext()
        recycler.also {
            it.layoutManager = LinearLayoutManager(context)
            adapter = TrackAdapter(this)
            it.adapter = adapter
        }
    }

    private fun applyPaletteTheme(palette: AlbumPalette) {
        album_info_layout.setBackgroundColor(palette.primary)
        title_view.setTextColor(palette.titleText)
        subtitle_view.setTextColor(palette.bodyText)

        val darkStatusText = palette.bodyText.luminance < 0.5f
        setDarkHomeUpIndicator(darkStatusText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity?.window?.darkSystemIcons = darkStatusText
        }

        collapsing_toolbar.setStatusBarScrimColor(palette.primaryDark)
        collapsing_toolbar.setContentScrimColor(palette.primary)

        play_fab.backgroundTintList = ColorStateList.valueOf(palette.accent)
        play_fab.imageTintList = ColorStateList.valueOf(palette.textOnAccent)
        decoration = CurrentlyPlayingDecoration(requireContext(), palette.accent)
        recycler.addItemDecoration(decoration)

    }

    private fun setDarkHomeUpIndicator(dark: Boolean) {
        val themedContext = ContextThemeWrapper(
            requireContext(),
            if (dark) R.style.ThemeOverlay_AppCompat_Light
            else R.style.ThemeOverlay_AppCompat_Dark_ActionBar
        )

        val upArrow = ContextCompat.getDrawable(themedContext, R.drawable.ic_arrow_back_24dp)
        (activity as? AppCompatActivity)?.supportActionBar?.setHomeAsUpIndicator(upArrow)
    }

    companion object Factory {
        const val ARG_ALBUM = "fr.nihilus.music.album.ALBUM"
        const val ARG_PALETTE = "fr.nihilus.music.album.PALETTE"

        const val ALBUM_ART_TRANSITION_NAME = "fr.nihilus.music.album.TRANSITION_NAME"

        fun newInstance(
            pickedAlbum: MediaBrowserCompat.MediaItem,
            palette: AlbumPalette
        ) = AlbumDetailFragment().apply {
            arguments = Bundle(2).apply {
                putParcelable(ARG_ALBUM, pickedAlbum)
                putParcelable(ARG_PALETTE, palette)
            }
        }
    }
}