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

package fr.nihilus.music.library.songs

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AbsListView.MultiChoiceModeListener
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.transition.TransitionManager
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.ConfirmDialogFragment
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.ProgressTimeLatch
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.library.HomeViewModel
import fr.nihilus.music.library.MusicLibraryViewModel
import fr.nihilus.music.library.playlists.AddToPlaylistDialog
import fr.nihilus.music.library.playlists.PlaylistActionResult
import fr.nihilus.music.library.playlists.PlaylistManagementViewModel
import fr.nihilus.music.ui.Stagger
import kotlinx.android.synthetic.main.fragment_songs.*

class SongListFragment : BaseFragment(R.layout.fragment_songs) {

    private val hostViewModel: MusicLibraryViewModel by activityViewModels()
    private val viewModel: HomeViewModel by viewModels(::requireParentFragment)
    private val playlistViewModel: PlaylistManagementViewModel by viewModels { viewModelFactory }

    private val multiSelectMode = SongListActionMode()
    private lateinit var songAdapter: SongAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        songAdapter = SongAdapter(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progressIndicator = view.findViewById<View>(R.id.progress_indicator)
        val songsListView = view.findViewById<ListView>(R.id.songs_listview)

        val progressBarLatch = ProgressTimeLatch { shouldShowProgress ->
            progressIndicator.isVisible = shouldShowProgress
            songsListView.isVisible = !shouldShowProgress
        }

        songsListView.apply {
            adapter = songAdapter
            choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
            setMultiChoiceModeListener(multiSelectMode)

            onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                val selectedTrack = songAdapter.getItem(position)
                hostViewModel.playMedia(selectedTrack)
            }
        }

        val staggerTransition = Stagger()

        viewModel.tracks.observe(this) { itemRequest ->
            when (itemRequest) {
                is LoadRequest.Pending -> progressBarLatch.isRefreshing = true
                is LoadRequest.Success -> {
                    progressBarLatch.isRefreshing = false
                    TransitionManager.beginDelayedTransition(songs_listview, staggerTransition)
                    songAdapter.submitList(itemRequest.data)
                    group_empty_view.isVisible = itemRequest.data.isEmpty()
                }
                is LoadRequest.Error -> {
                    progressBarLatch.isRefreshing = false
                    songAdapter.submitList(emptyList())
                    group_empty_view.isVisible = true
                }
            }
        }

        playlistViewModel.playlistActionResult.observe(this) { playlistEvent ->
            playlistEvent.handle { result ->
                when (result) {
                    is PlaylistActionResult.Created -> {
                        multiSelectMode.finish()
                        val userMessage = getString(R.string.playlist_created, result.playlistName)
                        Toast.makeText(context, userMessage, Toast.LENGTH_SHORT).show()
                    }

                    is PlaylistActionResult.Edited -> {
                        multiSelectMode.finish()
                        val userMessage = resources.getQuantityString(
                            R.plurals.tracks_added_to_playlist,
                            result.addedTracksCount,
                            result.addedTracksCount,
                            result.playlistName
                        )
                        Toast.makeText(context, userMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        val checkedItemCount = songs_listview.checkedItemCount

        val confirm = ConfirmDialogFragment.newInstance(
            this,
            R.id.request_delete_tracks,
            title = getString(R.string.delete_dialog_title),
            message = resources.getQuantityString(
                R.plurals.delete_dialog_message,
                checkedItemCount, checkedItemCount
            ),
            positiveButton = R.string.action_delete,
            negativeButton = R.string.cancel
        )
        confirm.show(requireFragmentManager(), null)
    }

    private fun deleteSelectedTracks() {
        val songsToDelete = getSelectedTrack()
        viewModel.deleteSongs(songsToDelete)
    }

    private fun openPlaylistChooserDialog() {
        val songsToAddToPlaylist = getSelectedTrack()
        val dialog = AddToPlaylistDialog.newInstance(this, songsToAddToPlaylist)
        dialog.show(requireFragmentManager(), AddToPlaylistDialog.TAG)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        // User has confirmed his intent to delete track(s)
        if (requestCode == R.id.request_delete_tracks && resultCode == DialogInterface.BUTTON_POSITIVE) {
            deleteSelectedTracks()
            multiSelectMode.finish()
        }
    }

    /**
     * An ActionMode that handles multiple item selection inside the song ListView.
     */
    private inner class SongListActionMode : MultiChoiceModeListener {
        private var actionMode: ActionMode? = null

        override fun onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long, checked: Boolean) {
            mode.title = songs_listview.checkedItemCount.toString()
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.actionmode_songlist, menu)
            actionMode = mode
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem) = when (item.itemId) {
            R.id.action_delete -> {
                showDeleteConfirmationDialog()
                true
            }
            R.id.action_playlist -> {
                openPlaylistChooserDialog()
                true
            }
            else -> false
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            // Items are deselected by default when action mode is destroyed
            actionMode = null
        }

        fun finish() {
            actionMode?.finish()
        }
    }

    private fun getSelectedTrack(): List<MediaBrowserCompat.MediaItem> {
        return if (songs_listview.choiceMode == ListView.CHOICE_MODE_MULTIPLE_MODAL) {
            val selectedTrackPositions = songs_listview.checkedItemPositions
            mutableListOf<MediaBrowserCompat.MediaItem>().also {
                for (pos in 0 until selectedTrackPositions.size()) {
                    if (selectedTrackPositions.valueAt(pos)) {
                        val itemAdapterPosition = selectedTrackPositions.keyAt(pos)
                        it.add(songAdapter.getItem(itemAdapterPosition))
                    }
                }
            }
        } else {
            emptyList()
        }
    }
}
