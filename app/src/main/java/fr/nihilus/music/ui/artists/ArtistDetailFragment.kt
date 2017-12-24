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

package fr.nihilus.music.ui.artists

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback
import android.support.v7.widget.GridLayoutManager
import android.view.View

import dagger.android.support.AndroidSupportInjection
import fr.nihilus.music.Constants
import fr.nihilus.music.R
import fr.nihilus.music.client.BrowserViewModel
import fr.nihilus.music.di.ActivityScoped
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.ui.albums.AlbumDetailActivity
import fr.nihilus.music.ui.holder.ArtistAlbumHolder
import fr.nihilus.recyclerfragment.RecyclerFragment

@ActivityScoped
class ArtistDetailFragment : RecyclerFragment(), BaseAdapter.OnItemSelectedListener {

    private lateinit var pickedArtist: MediaItem
    private lateinit var adapter: ArtistDetailAdapter

    private lateinit var viewModel: BrowserViewModel

    private val subscriptionCallback = object : SubscriptionCallback() {

        override fun onChildrenLoaded(parentId: String, children: List<MediaItem>) {
            adapter.update(children)
            setRecyclerShown(true)
        }
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = arguments
        pickedArtist = args?.getParcelable(KEY_ARTIST)
                ?: throw IllegalStateException("Caller must specify the artist to display.")

        adapter = ArtistDetailAdapter(this, this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val spanCount = resources.getInteger(R.integer.artist_grid_span_count)
        val manager = GridLayoutManager(context, spanCount)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = manager

        manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {

            override fun getSpanSize(position: Int): Int {
                val viewType = adapter.getItemViewType(position)
                return if (viewType == R.id.view_type_album) 1 else spanCount
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(activity!!).get(BrowserViewModel::class.java)
        setAdapter(adapter)
    }

    override fun onStart() {
        super.onStart()
        activity!!.title = pickedArtist.description.title
        viewModel.subscribe(pickedArtist.mediaId!!, subscriptionCallback)
    }

    override fun onStop() {
        viewModel.unsubscribe(pickedArtist.mediaId!!)
        super.onStop()
    }

    override fun onItemSelected(position: Int, actionId: Int) {
        val selectedItem = adapter[position]
        val holder = recyclerView.findViewHolderForAdapterPosition(position)
        when (holder.itemViewType) {
            R.id.view_type_track -> onTrackSelected(selectedItem)
            R.id.view_type_album -> onAlbumSelected(holder as ArtistAlbumHolder, selectedItem)
        }
    }

    private fun onAlbumSelected(holder: ArtistAlbumHolder, album: MediaItem) {
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                activity!!, holder.albumArt, AlbumDetailActivity.ALBUM_ART_TRANSITION_NAME)

        val albumDetailIntent = Intent(context, AlbumDetailActivity::class.java).apply {
            putExtra(AlbumDetailActivity.ARG_PICKED_ALBUM, album)
            putExtra(AlbumDetailActivity.ARG_PALETTE, holder.colors)
        }

        startActivity(albumDetailIntent, options.toBundle())
    }

    private fun onTrackSelected(track: MediaItem) {
        val mediaId = track.mediaId ?: throw AssertionError("Track should have a mediaId")
        viewModel.post { it.transportControls.playFromMediaId(mediaId, null) }
    }

    companion object Factory {

        private const val KEY_ARTIST = "artist"

        fun newInstance(artist: MediaItem) = ArtistDetailFragment().apply {
            arguments = Bundle(2).apply {
                putInt(Constants.FRAGMENT_ID, R.id.action_artists)
                putParcelable(KEY_ARTIST, artist)
            }
        }
    }
}