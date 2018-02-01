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

package fr.nihilus.music.ui.playlist

import android.app.Dialog
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import dagger.android.support.AndroidSupportInjection
import fr.nihilus.music.R
import fr.nihilus.music.client.BrowserViewModel
import fr.nihilus.music.di.ActivityScoped
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.glide.GlideRequest
import fr.nihilus.music.inflate
import fr.nihilus.music.utils.MediaID

@ActivityScoped
class AddToPlaylistDialog : AppCompatDialogFragment() {

    private lateinit var viewModel: BrowserViewModel
    private lateinit var playlistAdapter: ListAdapter

    companion object Factory {
        private const val ARG_SELECTED_TRACKS = "selected_tracks_ids"
        const val TAG = "AddToPlaylistDialog"

        fun newInstance(caller: Fragment, selectedTracksIds: LongArray) =
            AddToPlaylistDialog().apply {
                setTargetFragment(caller, 12)
                arguments = Bundle(1).apply {
                    putLongArray(ARG_SELECTED_TRACKS, selectedTracksIds)
                }
            }
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        playlistAdapter = ListAdapter(this)
        return AlertDialog.Builder(context!!)
            .setTitle(R.string.add_to_playlist)
            .setAdapter(playlistAdapter, dialogEventHandler)
            .setPositiveButton(R.string.action_create_playlist, dialogEventHandler)
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(activity!!).get(BrowserViewModel::class.java)
    }

    override fun onStart() {
        super.onStart()
        viewModel.subscribe(MediaID.ID_PLAYLISTS, updateItemsCallback)
    }

    override fun onStop() {
        viewModel.unsubscribe(MediaID.ID_PLAYLISTS)
        super.onStop()
    }

    private val dialogEventHandler = DialogInterface.OnClickListener { _, item ->
        when (item) {
            DialogInterface.BUTTON_POSITIVE -> TODO("Bouton neutre")
            else -> TODO("Position de l'élément cliqué dans l'adapter")
        }
    }

    private val updateItemsCallback = object : SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: List<MediaItem>) {
            playlistAdapter.update(children.filter {
                MediaID.getIdRoot(it.mediaId!!) == MediaID.ID_PLAYLISTS
            })
        }
    }

    private class ListAdapter(fragment: Fragment) : BaseAdapter() {

        private val items = ArrayList<MediaItem>()
        private val glideRequest = GlideApp.with(fragment).asBitmap().circleCrop()

        override fun getCount() = items.size

        override fun getItem(position: Int) = items[position]

        override fun getItemId(position: Int): Long {
            return if (hasStableIds()) {
                val mediaId = items[position].mediaId!!
                MediaID.extractBrowseCategoryValueFromMediaID(mediaId)?.toLong() ?: -1L
            } else -1L
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val itemView = convertView ?: onCreateView(parent)
            val holder = itemView.tag as PlaylistHolder

            holder.bind(items[position], glideRequest)
            return itemView
        }

        private fun onCreateView(parent: ViewGroup): View {
            return parent.inflate(R.layout.playlist_list_item).apply {
                tag = PlaylistHolder(this)
            }
        }

        fun update(playlists: List<MediaItem>) {
            items.clear()
            items += playlists
            notifyDataSetChanged()
        }
    }

    private class PlaylistHolder(itemView: View) {
        private val icon = itemView.findViewById<ImageView>(R.id.iconView)
        private val title = itemView.findViewById<TextView>(R.id.titleView)

        fun bind(playlist: MediaItem, glide: GlideRequest<*>) {
            title.text = playlist.description.title
            glide.load(playlist.description.iconUri).into(icon)
        }
    }
}