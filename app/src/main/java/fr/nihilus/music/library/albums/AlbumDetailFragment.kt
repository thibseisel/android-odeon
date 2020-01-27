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
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.Transition
import androidx.transition.TransitionInflater
import androidx.transition.TransitionListenerAdapter
import com.bumptech.glide.request.target.ImageViewTarget
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.extensions.darkSystemIcons
import fr.nihilus.music.core.ui.extensions.luminance
import fr.nihilus.music.extensions.doOnEnd
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.glide.palette.AlbumArt
import kotlinx.android.synthetic.main.fragment_album_detail.*
import java.util.concurrent.TimeUnit

/**
 * Display the tracks that are part of an album.
 */
class AlbumDetailFragment : BaseFragment(R.layout.fragment_album_detail) {

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

        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        play_fab.setOnClickListener {
            viewModel.playAlbum()
        }

        val adapter = TrackAdapter(viewModel::playTrack)
        recycler.adapter = adapter

        setDarkHomeUpIndicator(true)
        setDarkStatusBarIcons(true)

        viewModel.state.observe(viewLifecycleOwner) { albumDetail ->
            onAlbumDetailLoaded(albumDetail)
            adapter.submitList(albumDetail.tracks)
        }
    }

    private fun onAlbumDetailLoaded(album: AlbumDetailState) {
        title_view.text = album.title
        subtitle_view.text = album.subtitle

        GlideApp.with(this).asAlbumArt()
            .load(album.artworkUri)
            .error(R.drawable.ic_album_24dp)
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

            override fun onTransitionStart(transition: Transition) {
                // Hide the Floating Action Button at the beginning of the animation.
                play_fab?.isVisible = false
            }

            override fun onTransitionEnd(transition: Transition) {
                activity?.window?.statusBarColor = Color.TRANSPARENT

                // Show the Floating Action Button after transition is completed.
                play_fab?.show()
            }
        })
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