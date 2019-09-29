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
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.Transition
import androidx.transition.TransitionInflater
import androidx.transition.TransitionListenerAdapter
import com.bumptech.glide.request.target.ImageViewTarget
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.extensions.darkSystemIcons
import fr.nihilus.music.core.ui.extensions.luminance
import fr.nihilus.music.extensions.doOnEnd
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.glide.palette.AlbumArt
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.ui.CurrentlyPlayingDecoration
import kotlinx.android.synthetic.main.fragment_album_detail.*
import java.util.concurrent.TimeUnit

/**
 * Display the tracks that are part of an album.
 */
class AlbumDetailFragment : BaseFragment(R.layout.fragment_album_detail), BaseAdapter.OnItemSelectedListener {

    private lateinit var adapter: TrackAdapter
    private lateinit var decoration: CurrentlyPlayingDecoration

    private val viewModel: AlbumDetailViewModel by viewModels { viewModelFactory }
    private val args: AlbumDetailFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupSharedElementTransitions()
        viewModel.setAlbumId(args.albumId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // We are expecting an enter transition.
        postponeEnterTransition(500, TimeUnit.MILLISECONDS)

        // Set transition names.
        // Note that they don't need to match with the names of the selected grid item.
        // They only have to be unique in this fragment.
        album_art_view.transitionName = args.albumId

        play_fab.setOnClickListener {
            viewModel.playAlbum()
        }

        setupToolbar()
        setupTrackList()
        setDarkHomeUpIndicator(true)
        setDarkStatusBarIcons(true)

        viewModel.album.observe(viewLifecycleOwner, ::onAlbumDetailLoaded)

        // Change the decorated item when metadata changes
        viewModel.nowPlaying.observe(this, this::decoratePlayingTrack)

        // Subscribe to children of this album
        viewModel.tracks.observe(this) { trackUpdateRequest ->
            if (trackUpdateRequest is LoadRequest.Success) {
                adapter.submitList(trackUpdateRequest.data)
                val currentMetadata = viewModel.nowPlaying.value
                decoratePlayingTrack(currentMetadata)
            }
        }
    }

    private fun onAlbumDetailLoaded(album: MediaBrowserCompat.MediaItem) {
        val description = album.description
        title_view.text = description.title
        subtitle_view.text = description.subtitle

        GlideApp.with(this).asAlbumArt()
            .load(description.iconUri)
            .fallback(R.drawable.ic_album_24dp)
            .dontTransform()
            .doOnEnd(this::startPostponedEnterTransition)
            .into(object : ImageViewTarget<AlbumArt>(album_art_view) {

                override fun setResource(resource: AlbumArt?) {
                    if (resource != null) {
                        applyPaletteTheme(resource.palette)
                        super.view.setImageBitmap(resource.bitmap)
                    }
                }
            })
    }

    private fun setupSharedElementTransitions() {
        val inflater = TransitionInflater.from(requireContext())
        val albumArtTransition = inflater.inflateTransition(R.transition.album_art_transition)
        sharedElementEnterTransition = albumArtTransition

        albumArtTransition.addListener(object : TransitionListenerAdapter() {
            override fun onTransitionEnd(transition: Transition) {
                activity?.window?.statusBarColor = Color.TRANSPARENT
            }
        })
    }

    override fun onItemSelected(position: Int, actionId: Int) {
        val selectedTrack = adapter.getItem(position)
        viewModel.playTrack(selectedTrack)
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
        setDarkStatusBarIcons(darkStatusText)

        collapsing_toolbar.setStatusBarScrimColor(palette.primaryDark)
        collapsing_toolbar.setContentScrimColor(palette.primary)

        play_fab.backgroundTintList = ColorStateList.valueOf(palette.accent)
        play_fab.imageTintList = ColorStateList.valueOf(palette.textOnAccent)
        decoration = CurrentlyPlayingDecoration(requireContext(), palette.accent)
        recycler.addItemDecoration(decoration)
    }

    private fun setDarkStatusBarIcons(isDark: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity?.window?.darkSystemIcons = isDark
        }
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