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

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityOptionsCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import fr.nihilus.music.R
import fr.nihilus.music.base.BaseFragment
import fr.nihilus.music.dagger.ActivityScoped
import fr.nihilus.music.extensions.isVisible
import fr.nihilus.music.extensions.observeK
import fr.nihilus.music.library.FRAGMENT_ID
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.ui.LoadRequest
import fr.nihilus.music.ui.ProgressTimeLatch
import kotlinx.android.synthetic.main.fragment_albums.*
import javax.inject.Inject

@ActivityScoped
class AlbumGridFragment : BaseFragment(), BaseAdapter.OnItemSelectedListener {

    @Inject lateinit var defaultAlbumPalette: AlbumPalette

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(this, viewModelFactory)[AlbumGridViewModel::class.java]
    }

    private lateinit var albumAdapter: AlbumsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_albums, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val refreshToggle = ProgressTimeLatch { progressVisible ->
            progress_indicator.isVisible = progressVisible
        }

        albumAdapter =
                AlbumsAdapter(this, defaultAlbumPalette, this)
        with(album_recycler) {
            adapter = albumAdapter
            setHasFixedSize(true)
        }

        viewModel.albums.observeK(this) { albumRequest ->
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
        }
    }

    override fun onStart() {
        super.onStart()
        activity!!.setTitle(R.string.action_albums)
    }

    override fun onItemSelected(position: Int, actionId: Int) {
        val album = albumAdapter.getItem(position)
        val holder = album_recycler.findViewHolderForAdapterPosition(position) as AlbumHolder

        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            activity!!, holder.transitionView,
            AlbumDetailActivity.ALBUM_ART_TRANSITION_NAME
        )

        val albumDetailIntent = Intent(context, AlbumDetailActivity::class.java).apply {
            putExtra(AlbumDetailActivity.ARG_PICKED_ALBUM, album)
            putExtra(AlbumDetailActivity.ARG_PALETTE, holder.colorPalette)
        }

        startActivity(albumDetailIntent, options.toBundle())
    }

    companion object Factory {

        fun newInstance(): AlbumGridFragment {
            val args = Bundle(1)
            args.putInt(FRAGMENT_ID, R.id.action_albums)
            val fragment = AlbumGridFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
