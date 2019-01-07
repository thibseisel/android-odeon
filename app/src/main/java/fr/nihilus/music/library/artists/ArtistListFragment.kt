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

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import fr.nihilus.music.R
import fr.nihilus.music.base.BaseFragment
import fr.nihilus.music.dagger.ActivityScoped
import fr.nihilus.music.extensions.isVisible
import fr.nihilus.music.extensions.observeK
import fr.nihilus.music.library.FRAGMENT_ID
import fr.nihilus.music.library.NavigationController
import fr.nihilus.music.library.artists.detail.ArtistAdapter
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.ui.LoadRequest
import fr.nihilus.music.ui.ProgressTimeLatch
import kotlinx.android.synthetic.main.fragment_artists.*
import javax.inject.Inject

@ActivityScoped
class ArtistListFragment : BaseFragment(), BaseAdapter.OnItemSelectedListener {
    @Inject lateinit var router: NavigationController

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(this, viewModelFactory)[ArtistListViewModel::class.java]
    }

    private lateinit var adapter: ArtistAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = ArtistAdapter(this, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_artists, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progressBarLatch = ProgressTimeLatch { shouldShow ->
            progress_indicator.isVisible = shouldShow
        }

        adapter = ArtistAdapter(this, this)

        viewModel.artists.observeK(this) { artistRequest ->
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

    override fun onStart() {
        super.onStart()
        activity!!.setTitle(R.string.action_artists)
    }

    override fun onItemSelected(position: Int, actionId: Int) {
        val artist = adapter.getItem(position)
        router.navigateToArtistDetail(artist)
    }

    companion object Factory {
        fun newInstance() = ArtistListFragment().apply {
            arguments = Bundle(1).apply {
                putInt(FRAGMENT_ID, R.id.action_artists)
            }
        }
    }
}
