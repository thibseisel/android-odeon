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

package fr.nihilus.music.library

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.MaterialSharedAxis
import dagger.hilt.android.AndroidEntryPoint
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.databinding.FragmentHomeBinding
import fr.nihilus.music.library.albums.AlbumsFragment
import fr.nihilus.music.library.artists.ArtistsFragment
import fr.nihilus.music.library.playlists.PlaylistsFragment
import fr.nihilus.music.library.songs.AllTracksFragment
import java.util.concurrent.TimeUnit
import fr.nihilus.music.core.ui.R as CoreUiR

/**
 * Host fragment for displaying collections of media: all tracks, albums, artists and user-defined playlists.
 * Each collection is contained in a tab.
 */
@AndroidEntryPoint
class HomeFragment : BaseFragment(R.layout.fragment_home) {
    private val viewModel by viewModels<HomeViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentHomeBinding.bind(view)

        // Postpone transition when returning from album detail.
        postponeEnterTransition(1000, TimeUnit.MILLISECONDS)

        // Configure toolbar with title and menu.
        binding.toolbar.run {
            setTitle(fr.nihilus.music.core.R.string.core_app_name)
            inflateMenu(R.menu.menu_home)
            setOnMenuItemClickListener(::onOptionsItemSelected)
        }

        // Configure tabs and ViewPager.
        val pagerAdapter = MusicLibraryTabAdapter(this)
        binding.fragmentPager.adapter = pagerAdapter
        binding.fragmentPager.offscreenPageLimit = 1
        TabLayoutMediator(binding.tabHost, binding.fragmentPager, false) { tab, position ->
            tab.icon = pagerAdapter.getIcon(position)
            tab.contentDescription = pagerAdapter.getTitle(position)
        }.attach()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_search -> {
            navigateToSearch()
            true
        }

        R.id.action_shuffle -> {
            viewModel.playAllShuffled()
            true
        }

        R.id.start_cleanup -> {
            findNavController().navigate(R.id.start_cleanup)
            true
        }

        R.id.activity_settings -> {
            findNavController().navigate(R.id.activity_settings)
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    private fun navigateToSearch() {
        val transitionDuration =
            resources.getInteger(CoreUiR.integer.ui_motion_duration_large).toLong()
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            duration = transitionDuration
        }
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
            duration = transitionDuration
        }

        findNavController().navigate(HomeFragmentDirections.actionHomeToSearch())
    }

    /**
     * An adapter that maps fragments displaying collection of media to items in a ViewPager.
     */
    private class MusicLibraryTabAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        private val context = fragment.requireContext()

        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> AllTracksFragment()
            1 -> AlbumsFragment()
            2 -> ArtistsFragment()
            3 -> PlaylistsFragment()
            else -> error("Requested a Fragment for a tab at unexpected position: $position")
        }

        fun getTitle(position: Int): String? = when (position) {
            0 -> context.getString(R.string.all_music)
            1 -> context.getString(R.string.action_albums)
            2 -> context.getString(R.string.action_artists)
            3 -> context.getString(R.string.action_playlists)
            else -> null
        }

        fun getIcon(position: Int): Drawable? = when (position) {
            0 -> ContextCompat.getDrawable(context, R.drawable.ic_audiotrack_24dp)
            1 -> ContextCompat.getDrawable(context, R.drawable.ic_album_24dp)
            2 -> ContextCompat.getDrawable(context, R.drawable.ic_person_24dp)
            3 -> ContextCompat.getDrawable(context, R.drawable.ic_playlist_24dp)
            else -> null
        }
    }
}
