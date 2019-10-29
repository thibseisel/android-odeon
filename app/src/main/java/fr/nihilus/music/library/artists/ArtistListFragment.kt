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

package fr.nihilus.music.library.artists

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.ProgressTimeLatch
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.library.HomeFragmentDirections
import fr.nihilus.music.library.HomeViewModel
import fr.nihilus.music.library.artists.detail.ArtistAdapter
import fr.nihilus.music.ui.BaseAdapter
import kotlinx.android.synthetic.main.fragment_artists.*

class ArtistListFragment : BaseFragment(R.layout.fragment_artists), BaseAdapter.OnItemSelectedListener {
    private val viewModel: HomeViewModel by viewModels(::requireParentFragment)

    private lateinit var adapter: ArtistAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = ArtistAdapter(this, this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progressIndicator = view.findViewById<View>(R.id.progress_indicator)
        val progressBarLatch = ProgressTimeLatch { shouldShow ->
            progressIndicator.isVisible = shouldShow
        }

        artist_recycler.adapter = adapter
        artist_recycler.setHasFixedSize(true)

        viewModel.artists.observe(this) { artistRequest ->
            when (artistRequest) {
                is LoadRequest.Pending -> progressBarLatch.isRefreshing = true
                is LoadRequest.Success -> {
                    progressBarLatch.isRefreshing = false
                    adapter.submitList(artistRequest.data)
                    group_empty_view.isVisible = artistRequest.data.isEmpty()
                }
                is LoadRequest.Error -> {
                    progressBarLatch.isRefreshing = false
                    adapter.submitList(emptyList())
                    group_empty_view.isVisible = true
                }
            }
        }
    }

    override fun onItemSelected(position: Int, actionId: Int) {
        val artist = adapter.getItem(position)
        val toArtistDetail = HomeFragmentDirections.browseArtistDetail(artist.mediaId!!)
        findNavController().navigate(toArtistDetail)
    }
}
