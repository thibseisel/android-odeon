/*
 * Copyright 2020 Thibault Seisel
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
import android.os.Bundle
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.graphics.luminance
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.Transition
import androidx.transition.TransitionListenerAdapter
import com.bumptech.glide.request.target.ImageViewTarget
import com.google.android.material.transition.MaterialContainerTransform
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.extensions.resolveThemeColor
import fr.nihilus.music.core.ui.glide.GlideApp
import fr.nihilus.music.core.ui.glide.palette.AlbumArt
import fr.nihilus.music.core.ui.glide.palette.AlbumPalette
import fr.nihilus.music.databinding.FragmentAlbumDetailBinding
import fr.nihilus.music.core.ui.extensions.startPostponedEnterTransitionWhenDrawn
import java.util.concurrent.TimeUnit

/**
 * Display the tracks that are part of an album.
 */
class AlbumDetailFragment : BaseFragment(R.layout.fragment_album_detail) {

    private val viewModel: AlbumDetailViewModel by viewModels { viewModelFactory }
    private val args: AlbumDetailFragmentArgs by navArgs()

    private var binding: FragmentAlbumDetailBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupSharedElementTransitions()
        viewModel.setAlbumId(args.albumId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentAlbumDetailBinding.bind(view)
        this.binding = binding

        // Start shared element enter transition only after track list have been loaded.
        postponeEnterTransition(500, TimeUnit.MILLISECONDS)

        // Set transition names.
        // Note that they don't need to match with the names of the selected grid item.
        // They only have to be unique in this fragment.
        view.transitionName = args.albumId

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.playFab.setOnClickListener {
            viewModel.playAlbum()
        }

        val adapter = TrackAdapter(viewModel::playTrack)
        binding.recycler.adapter = adapter

        viewModel.state.observe(viewLifecycleOwner) { albumDetail ->
            onAlbumDetailLoaded(albumDetail, binding)
            adapter.submitList(albumDetail.tracks)
            startPostponedEnterTransitionWhenDrawn()
        }
    }

    private fun onAlbumDetailLoaded(album: AlbumDetailState, binding: FragmentAlbumDetailBinding) {
        binding.titleView.text = album.title
        binding.subtitleView.text = album.subtitle

        // Note: Glide is attached to the context of the activity to workaround a bug in
        // MaterialContainerTransform not capturing images in return transition.
        GlideApp.with(requireActivity()).asAlbumArt()
            .load(album.artworkUri)
            .error(R.drawable.ic_album_24dp)
            .dontTransform()
            .disallowHardwareConfig()
            .into(object : ImageViewTarget<AlbumArt>(binding.albumArtView) {

                override fun setResource(resource: AlbumArt?) {
                    if (resource != null) {
                        applyPaletteTheme(resource.palette, binding)
                        super.view.setImageBitmap(resource.bitmap)
                    }
                }
            })
    }

    private fun setupSharedElementTransitions() {
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            // Animate behind player sheet and status bar.
            drawingViewId = R.id.nav_host_fragment
            duration = resources.getInteger(R.integer.ui_motion_duration_large).toLong()
            // Draw a background color behind the track list to prevent from drawing
            // the seeing the previous fragment beneath.
            val themeColorSurface = resolveThemeColor(requireContext(), R.attr.colorSurface)
            setAllContainerColors(themeColorSurface)

            addListener(object : TransitionListenerAdapter() {

                override fun onTransitionStart(transition: Transition) {
                    // Hide the Floating Action Button at the beginning of the animation.
                    binding?.playFab?.isVisible = false
                }

                override fun onTransitionEnd(transition: Transition) {
                    // Show the Floating Action Button after transition is completed.
                    binding?.playFab?.show()
                }
            })
        }
    }

    private fun applyPaletteTheme(palette: AlbumPalette, binding: FragmentAlbumDetailBinding) {
        binding.albumInfoLayout.setBackgroundColor(palette.primary)
        binding.titleView.setTextColor(palette.titleText)
        binding.subtitleView.setTextColor(palette.bodyText)

        val darkStatusText = palette.primary.luminance > 0.5f
        setDarkHomeUpIndicator(darkStatusText, binding)

        binding.collapsingToolbar.setStatusBarScrimColor(palette.primaryDark)
        binding.collapsingToolbar.setContentScrimColor(palette.primary)

        binding.playFab.backgroundTintList = ColorStateList.valueOf(palette.accent)
        binding.playFab.imageTintList = ColorStateList.valueOf(palette.textOnAccent)
    }

    private fun setDarkHomeUpIndicator(dark: Boolean, binding: FragmentAlbumDetailBinding) {
        val themedContext = ContextThemeWrapper(
            requireContext(),
            if (dark) R.style.ThemeOverlay_AppCompat_Light
            else R.style.ThemeOverlay_AppCompat_Dark_ActionBar
        )

        val upArrow = ContextCompat.getDrawable(themedContext, R.drawable.ic_arrow_back_24dp)
        binding.toolbar.navigationIcon = upArrow
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}