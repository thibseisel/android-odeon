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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import fr.nihilus.music.R
import fr.nihilus.music.base.BaseFragment
import fr.nihilus.music.extensions.afterMeasure
import fr.nihilus.music.extensions.isVisible
import fr.nihilus.music.library.HomeFragmentDirections
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.ui.LoadRequest
import fr.nihilus.music.ui.ProgressTimeLatch
import kotlinx.android.synthetic.main.fragment_albums.*
import javax.inject.Inject

class AlbumGridFragment : BaseFragment(), BaseAdapter.OnItemSelectedListener {

    @Inject lateinit var defaultAlbumPalette: AlbumPalette

    private val viewModel: AlbumGridViewModel by viewModels { viewModelFactory }
    private lateinit var albumAdapter: AlbumsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_albums, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progressIndicator = view.findViewById<View>(R.id.progress_indicator)
        val refreshToggle = ProgressTimeLatch { progressVisible ->
            progressIndicator.isVisible = progressVisible
        }

        albumAdapter = AlbumsAdapter(this, defaultAlbumPalette, this)
        album_recycler.apply {
            adapter = albumAdapter
            setHasFixedSize(true)
            afterMeasure { requireParentFragment().startPostponedEnterTransition() }
        }

        viewModel.children.observe(this, { albumRequest ->
            when (albumRequest) {
                is LoadRequest.Pending -> refreshToggle.isRefreshing = true
                is LoadRequest.Success -> {
                    refreshToggle.isRefreshing = false
                    albumAdapter.submitList(albumRequest.data)
                    group_empty_view.isVisible = albumRequest.data.isEmpty()
                }
                is LoadRequest.Error -> {
                    refreshToggle.isRefreshing = false
                    albumAdapter.submitList(emptyList())
                    group_empty_view.isVisible = true
                }
            }
        })
    }

    override fun onItemSelected(position: Int, actionId: Int) {
        val album = albumAdapter.getItem(position)
        val holder = album_recycler.findViewHolderForAdapterPosition(position) as AlbumHolder

        val toAlbumDetail = HomeFragmentDirections.browseAlbumDetail(album, holder.colorPalette)
        val transitionExtras = FragmentNavigatorExtras(
            holder.transitionView to album.mediaId!!
        )

        findNavController().navigate(toAlbumDetail, transitionExtras)
    }
}
