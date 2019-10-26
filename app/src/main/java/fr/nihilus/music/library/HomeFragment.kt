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

package fr.nihilus.music.library

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.*
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.library.albums.AlbumGridFragment
import fr.nihilus.music.library.artists.ArtistListFragment
import fr.nihilus.music.library.playlists.PlaylistsFragment
import fr.nihilus.music.library.songs.SongListFragment
import kotlinx.android.synthetic.main.fragment_home.*
import java.util.concurrent.TimeUnit

/**
 * Host fragment for displaying collections of media: all tracks, albums, artists and user-defined playlists.
 * Each collection is contained in a tab.
 */
class HomeFragment : BaseFragment(R.layout.fragment_home) {
    private val activityViewModel by activityViewModels<MusicLibraryViewModel>()
    private val viewModel by viewModels<HomeViewModel> { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Postpone transition when returning from album detail.
        postponeEnterTransition(300L, TimeUnit.MILLISECONDS)
        allowReturnTransitionOverlap = true

        // Configure toolbar with title and menu.
        toolbar.run {
            setTitle(R.string.core_app_name)
            prepareMenu()
        }

        // Configure tabs and ViewPager.
        tab_host.setupWithViewPager(fragment_pager)
        fragment_pager.adapter = MusicLibraryTabAdapter(requireContext(), childFragmentManager)

        viewModel.deleteTracksConfirmation.observe(this) { toastMessageEvent ->
            toastMessageEvent.handle { deletedTracksCount ->
                val message = resources.getQuantityString(
                    R.plurals.deleted_songs_confirmation,
                    deletedTracksCount,
                    deletedTracksCount
                )

                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun Toolbar.prepareMenu() {
        inflateMenu(R.menu.menu_home)
        setOnMenuItemClickListener(::onOptionsItemSelected)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.onNavDestinationSelected(findNavController())) {
            return true
        }

        return when (item.itemId) {
            R.id.action_shuffle -> {
                viewModel.playAllShuffled()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * An adapter that maps fragments displaying collection of media to items in a ViewPager.
     */
    private class MusicLibraryTabAdapter(
        private val context: Context,
        fm: FragmentManager
    ) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getCount(): Int = 4

        override fun getItem(position: Int): Fragment = when (position) {
            0 -> SongListFragment()
            1 -> AlbumGridFragment()
            2 -> ArtistListFragment()
            3 -> PlaylistsFragment()
            else -> error("Requested a Fragment for a tab at unexpected position: $position")
        }

        override fun getPageTitle(position: Int): CharSequence? = when (position) {
            0 -> context.getString(R.string.all_music)
            1 -> context.getString(R.string.action_albums)
            2 -> context.getString(R.string.action_artists)
            3 -> context.getString(R.string.action_playlists)
            else -> null
        }
    }
}