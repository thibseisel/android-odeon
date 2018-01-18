/*
 * Copyright 2017 Thibault Seisel
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

package fr.nihilus.music.ui.albums

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.AndroidSupportInjection
import fr.nihilus.music.Constants
import fr.nihilus.music.R
import fr.nihilus.music.client.BrowserViewModel
import fr.nihilus.music.di.ActivityScoped
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.ui.holder.AlbumHolder
import fr.nihilus.music.utils.MediaID
import fr.nihilus.recyclerfragment.RecyclerFragment

@ActivityScoped
class AlbumGridFragment : RecyclerFragment(), BaseAdapter.OnItemSelectedListener {

    private lateinit var adapter: AlbumsAdapter
    private lateinit var viewModel: BrowserViewModel

    private val subscriptionCallback = object : SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, albums: List<MediaItem>) {
            adapter.update(albums)
            setRecyclerShown(true)
        }
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = AlbumsAdapter(this, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.fragment_albums, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(activity!!).get(BrowserViewModel::class.java)

        setAdapter(adapter)
        recyclerView.setHasFixedSize(true)

        if (savedInstanceState == null) {
            // Show progress indicator while loading album items
            setRecyclerShown(false)
        }
    }

    override fun onStart() {
        super.onStart()
        activity!!.setTitle(R.string.action_albums)
        viewModel.subscribe(MediaID.ID_ALBUMS, subscriptionCallback)
    }

    override fun onStop() {
        viewModel.unsubscribe(MediaID.ID_ALBUMS)
        super.onStop()
    }

    override fun onItemSelected(position: Int, actionId: Int) {
        val album = adapter[position]
        val holder = recyclerView.findViewHolderForAdapterPosition(position) as AlbumHolder

        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            activity!!, holder.transitionView, AlbumDetailActivity.ALBUM_ART_TRANSITION_NAME
        )
        val albumDetailIntent = Intent(context, AlbumDetailActivity::class.java)
        albumDetailIntent.putExtra(AlbumDetailActivity.ARG_PICKED_ALBUM, album)
        albumDetailIntent.putExtra(AlbumDetailActivity.ARG_PALETTE, holder.colors)
        startActivity(albumDetailIntent, options.toBundle())
    }

    companion object Factory {

        fun newInstance(): AlbumGridFragment {
            val args = Bundle(1)
            args.putInt(Constants.FRAGMENT_ID, R.id.action_albums)
            val fragment = AlbumGridFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
