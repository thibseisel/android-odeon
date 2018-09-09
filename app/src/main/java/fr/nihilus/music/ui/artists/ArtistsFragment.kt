/*
 * Copyright 2018 Thibault Seisel
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

package fr.nihilus.music.ui.artists

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.AndroidSupportInjection
import fr.nihilus.music.R
import fr.nihilus.music.client.BrowserViewModel
import fr.nihilus.music.client.NavigationController
import fr.nihilus.music.di.ActivityScoped
import fr.nihilus.music.media.CATEGORY_ARTISTS
import fr.nihilus.music.media.Constants
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.recyclerfragment.RecyclerFragment
import javax.inject.Inject

@ActivityScoped
class ArtistsFragment : RecyclerFragment(), BaseAdapter.OnItemSelectedListener {

    @Inject lateinit var router: NavigationController

    private lateinit var adapter: ArtistAdapter
    private lateinit var viewModel: BrowserViewModel

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = ArtistAdapter(this, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_artists, container, false)

    override fun onStart() {
        super.onStart()
        activity!!.setTitle(R.string.action_artists)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(activity!!).get(BrowserViewModel::class.java)
        viewModel.subscribeTo(CATEGORY_ARTISTS).observe(this, Observer {
            adapter.submitList(it.orEmpty())
            setRecyclerShown(true)
        })

        setAdapter(adapter)
        recyclerView.setHasFixedSize(true)

        if (savedInstanceState == null) {
            setRecyclerShown(false)
        }
    }

    override fun onItemSelected(position: Int, actionId: Int) {
        val artist = adapter.getItem(position)
        router.navigateToArtistDetail(artist)
    }

    companion object Factory {

        fun newInstance() = ArtistsFragment().apply {
            arguments = Bundle(1).apply {
                putInt(Constants.FRAGMENT_ID, R.id.action_artists)
            }
        }
    }
}
