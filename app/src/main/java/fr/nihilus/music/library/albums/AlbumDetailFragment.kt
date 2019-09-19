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
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.view.View
import android.widget.ImageView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.Transition
import androidx.transition.TransitionInflater
import androidx.transition.TransitionListenerAdapter
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.extensions.darkSystemIcons
import fr.nihilus.music.extensions.luminance
import fr.nihilus.music.extensions.resolveDefaultAlbumPalette
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.ui.CurrentlyPlayingDecoration
import kotlinx.android.synthetic.main.fragment_album_detail.*

/**
 * Display the tracks that are part of an album.
 */
class AlbumDetailFragment : BaseFragment(R.layout.fragment_album_detail), BaseAdapter.OnItemSelectedListener {

    private lateinit var adapter: TrackAdapter
    private lateinit var decoration: CurrentlyPlayingDecoration

    private val viewModel: AlbumDetailViewModel by viewModels { viewModelFactory }
    private val args: AlbumDetailFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSharedElementTransitions()

        viewModel.loadTracksOfAlbum(args.pickedAlbum)

        with(args.pickedAlbum.description) {
            title_view.text = title
            subtitle_view.text = subtitle
        }

        play_fab.setOnClickListener {
            viewModel.playMedia(args.pickedAlbum)
        }

        setupToolbar()
        setupAlbumArt()
        setupTrackList()

        val colorPalette = args.albumPalette ?: requireContext().resolveDefaultAlbumPalette()
        applyPaletteTheme(colorPalette)

        // Change the decorated item when metadata changes
        viewModel.nowPlaying.observe(this, this::decoratePlayingTrack)

        // Subscribe to children of this album
        viewModel.children.observe(this) { trackUpdateRequest ->
            if (trackUpdateRequest is LoadRequest.Success) {
                adapter.submitList(trackUpdateRequest.data)
                val currentMetadata = viewModel.nowPlaying.value
                decoratePlayingTrack(currentMetadata)
            }
        }
    }

    private fun setupSharedElementTransitions() {
        val inflater = TransitionInflater.from(requireContext())
        val albumArtTransition = inflater.inflateTransition(android.R.transition.move)
        sharedElementEnterTransition = albumArtTransition

        albumArtTransition.addListener(object : TransitionListenerAdapter() {
            override fun onTransitionEnd(transition: Transition) {
                activity?.window?.statusBarColor = Color.TRANSPARENT
            }
        })
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

    private fun setupToolbar() {
        toolbar.apply {
            title = null
            setNavigationOnClickListener { findNavController().navigateUp() }
        }
    }

    private fun setupAlbumArt() {
        val albumArtView: ImageView = album_art_view
        albumArtView.transitionName = args.pickedAlbum.mediaId

        GlideApp.with(this).asBitmap()
            .load(args.pickedAlbum.description.iconUri)
            .fallback(R.drawable.ic_album_24dp)
            .centerCrop()
            .into(albumArtView)
    }

    private fun setupTrackList() {
        recycler.also {
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
        toolbar.navigationIcon = upArrow
    }
}