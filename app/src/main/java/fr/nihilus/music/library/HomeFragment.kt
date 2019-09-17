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
import android.support.v4.media.MediaBrowserCompat
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import fr.nihilus.music.R
import fr.nihilus.music.common.media.MediaId
import fr.nihilus.music.common.media.toMediaId
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.library.albums.AlbumGridFragment
import fr.nihilus.music.library.artists.ArtistListFragment
import fr.nihilus.music.library.playlists.PlaylistsFragment
import fr.nihilus.music.library.songs.SongListFragment
import fr.nihilus.music.view.SearchInputView
import kotlinx.android.synthetic.main.fragment_home.*
import java.util.concurrent.TimeUnit

/**
 * Host fragment for displaying collections of media: all tracks, albums, artists and user-defined playlists.
 * Each collection is contained in a tab.
 */
class HomeFragment : BaseFragment(R.layout.fragment_home) {

    private val viewModel by activityViewModels<MusicLibraryViewModel>()
    private lateinit var suggestionsAdapter: SearchSuggestionsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Postpone transition when returning from album detail.
        postponeEnterTransition(300L, TimeUnit.MILLISECONDS)
        allowReturnTransitionOverlap = true

        suggestionsAdapter = SearchSuggestionsAdapter(this)

        // Configure toolbar with title and menu.
        toolbar.run {
            setTitle(R.string.core_app_name)
            prepareMenu()
        }

        // Configure tabs and ViewPager.
        tab_host.setupWithViewPager(fragment_pager)
        fragment_pager.adapter = MusicLibraryTabAdapter(requireContext(), childFragmentManager)

        viewModel.searchResults.observe(this) { searchResults ->
            suggestionsAdapter.submitList(searchResults)
        }
    }

    private fun Toolbar.prepareMenu() {
        inflateMenu(R.menu.menu_home)
        setOnMenuItemClickListener(::onOptionsItemSelected)

        val actionView = menu.findItem(R.id.action_search).actionView as SearchInputView
        actionView.setQueryHint(R.string.search_hint)
        actionView.setAdapter(suggestionsAdapter)
        actionView.doAfterTextChanged { text ->
            viewModel.search(text ?: "")
        }

        actionView.setOnSuggestionSelected { position ->
            val selectedItem = suggestionsAdapter.getItem(position)
            handleSelectedSearchResult(selectedItem)
        }
    }

    private fun handleSelectedSearchResult(item: MediaBrowserCompat.MediaItem) {
        when {
            item.isBrowsable -> browseMedia(item)
            item.isPlayable -> viewModel.playMedia(item)
        }
    }

    private fun browseMedia(item: MediaBrowserCompat.MediaItem) {
        val navController = findNavController()
        val (type, _, _) = item.mediaId.toMediaId()

        when (type) {
            MediaId.TYPE_ALBUMS -> {
                val toAlbumDetail = HomeFragmentDirections.browseAlbumDetail(item, null)
                navController.navigate(toAlbumDetail)
            }

            MediaId.TYPE_ARTISTS -> {
                val toArtistDetail = HomeFragmentDirections.browseArtistDetail(item)
                navController.navigate(toArtistDetail)
            }

            MediaId.TYPE_PLAYLISTS -> {
                val toPlaylistContent = HomeFragmentDirections.browsePlaylistContent(item)
                navController.navigate(toPlaylistContent)
            }
        }
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