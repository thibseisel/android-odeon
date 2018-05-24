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

import android.app.Activity
import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.media.MediaBrowserCompat.MediaItem
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
import fr.nihilus.music.command.EditPlaylistCommand
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.glide.GlideRequest
import fr.nihilus.music.inflate
import fr.nihilus.music.media.CATEGORY_PLAYLISTS
import fr.nihilus.music.media.musicIdFrom
import fr.nihilus.music.utils.MediaID

/**
 * A fragment displaying an Alert Dialog prompting the user to choose to which playlists
 * he wants to add a set of tracks.
 * He may also trigger another dialog allowing him to create a new playlist from the given tracks.
 */
class AddToPlaylistDialog : AppCompatDialogFragment() {

    private lateinit var browserViewModel: BrowserViewModel
    private lateinit var playlistAdapter: ListAdapter

    companion object Factory {
        private const val ARG_SELECTED_TRACKS = "selected_tracks_ids"

        /**
         * The tag associated with this dialog.
         * This may be used to identify the dialog in the fragment manager.
         */
        const val TAG = "AddToPlaylistDialog"

        /**
         * The number of tracks that have been added to the playlist
         * as a result of calling this dialog.
         * This is passed as an extra in the caller's [Fragment.onActivityResult].
         *
         * Type: `Int`
         */
        const val RESULT_TRACK_COUNT = "track_count"

        /**
         * The title of the playlist the tracks have been added to
         * as a result of calling this dialog.
         * This is passed as an extra in the caller's [Fragment.onActivityResult].
         *
         * Type: `String`
         */
        const val RESULT_PLAYLIST_TITLE = "playlist_title"

        fun newInstance(caller: Fragment, requestCode: Int, selectedTracksIds: LongArray) =
            AddToPlaylistDialog().apply {
                setTargetFragment(caller, requestCode)
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

        browserViewModel = ViewModelProviders.of(activity!!).get(BrowserViewModel::class.java)
        browserViewModel.subscribeTo(CATEGORY_PLAYLISTS).observe(this, Observer {
            val children = it?.filter {
                MediaID.getIdRoot(it.mediaId!!) == CATEGORY_PLAYLISTS
            }.orEmpty()
            playlistAdapter.update(children)
        })
    }

    private val dialogEventHandler = DialogInterface.OnClickListener { _, position ->
        if (position >= 0) {
            // The clicked element is a playlist
            val playlist = playlistAdapter.getItem(position)
            val trackIds = arguments?.getLongArray(ARG_SELECTED_TRACKS) ?: LongArray(0)
            performAddToPlaylist(playlist, trackIds)

        } else if (position == DialogInterface.BUTTON_POSITIVE) {
            // The "New playlist" action has been selected
            callNewPlaylistDialog()
        }
    }

    private fun performAddToPlaylist(playlist: MediaItem, newTrackIds: LongArray) {

        val playlistId = MediaID.categoryValueOf(playlist.mediaId!!).toLong()
        val params = Bundle(2).apply {
            putLong(EditPlaylistCommand.PARAM_PLAYLIST_ID, playlistId)
            putLongArray(EditPlaylistCommand.PARAM_NEW_TRACKS, newTrackIds)
        }

        browserViewModel.postCommand(EditPlaylistCommand.CMD_NAME, params) { resultCode, _ ->
            // Only notify clients when the operation is successful
            if (resultCode == R.id.result_success) {
                targetFragment?.onActivityResult(
                    targetRequestCode,
                    Activity.RESULT_OK,
                    Intent().apply {
                        putExtra(RESULT_TRACK_COUNT, newTrackIds.size)
                        putExtra(RESULT_PLAYLIST_TITLE, playlist.description.title ?: "")
                    }
                )
            }
        }
    }

    private fun callNewPlaylistDialog() {
        val memberTracks = arguments?.getLongArray(ARG_SELECTED_TRACKS) ?: LongArray(0)
        NewPlaylistDialog.newInstance(targetFragment!!, targetRequestCode, memberTracks)
            .show(fragmentManager, NewPlaylistDialog.TAG)
    }

    override fun onCancel(dialog: DialogInterface?) {
        targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_CANCELED, null)
    }

    /**
     * The adapter used to display available playlists in the dialog's body.
     */
    private class ListAdapter(fragment: Fragment) : BaseAdapter() {

        private val items = ArrayList<MediaItem>()
        private val glideRequest = GlideApp.with(fragment).asBitmap().circleCrop()

        override fun getCount() = items.size

        override fun getItem(position: Int) = items[position]

        override fun getItemId(position: Int): Long {
            return if (hasStableIds()) {
                val mediaId = items[position].mediaId!!
                musicIdFrom(mediaId)?.toLong() ?: -1L
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

    /**
     * Holds references to views representing a playlist item.
     */
    private class PlaylistHolder(itemView: View) {
        private val icon = itemView.findViewById<ImageView>(R.id.iconView)
        private val title = itemView.findViewById<TextView>(R.id.titleView)

        fun bind(playlist: MediaItem, glide: GlideRequest<*>) {
            title.text = playlist.description.title
            glide.load(playlist.description.iconUri).into(icon)
        }
    }
}