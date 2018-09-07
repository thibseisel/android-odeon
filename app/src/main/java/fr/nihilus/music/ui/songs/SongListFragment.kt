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

package fr.nihilus.music.ui.songs

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import android.widget.AbsListView.MultiChoiceModeListener
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import dagger.android.support.AndroidSupportInjection
import fr.nihilus.music.R
import fr.nihilus.music.client.BrowserViewModel
import fr.nihilus.music.media.command.DeleteTracksCommand
import fr.nihilus.music.media.CATEGORY_MUSIC
import fr.nihilus.music.media.Constants
import fr.nihilus.music.ui.playlist.AddToPlaylistDialog
import fr.nihilus.music.ui.playlist.NewPlaylistDialog
import fr.nihilus.music.utils.ConfirmDialogFragment
import kotlinx.android.synthetic.main.fragment_songs.*

class SongListFragment : Fragment(),
    AdapterView.OnItemClickListener {

    private val multiSelectMode = SongListActionMode()

    private lateinit var songAdapter: SongAdapter
    private lateinit var viewModel: BrowserViewModel

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        songAdapter = SongAdapter(this)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_songlist, menu)
        //val searchView = menu.findItem(R.id.action_search).actionView as SearchView
        // TODO Search and filtering features
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_play_shuffled -> {
            viewModel.post { controller ->
                controller.transportControls.playFromMediaId(CATEGORY_MUSIC, Bundle(1).apply {
                    putBoolean(Constants.EXTRA_PLAY_SHUFFLED, true)
                })
            }
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_songs, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(list) {
            adapter = songAdapter
            onItemClickListener = this@SongListFragment
            emptyView = view.findViewById(android.R.id.empty)
            choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
            setMultiChoiceModeListener(multiSelectMode)
        }

        listContainer.visibility = View.GONE
        if (savedInstanceState == null) {
            progress.show()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(activity!!).get(BrowserViewModel::class.java)
        viewModel.subscribeTo(CATEGORY_MUSIC).observe(this, Observer {
            songAdapter.updateItems(it.orEmpty())
            progress.hide()
            listContainer.visibility = View.VISIBLE
        })
    }

    override fun onStart() {
        super.onStart()
        activity!!.setTitle(R.string.all_music)
    }

    override fun onItemClick(listView: AdapterView<*>, view: View, position: Int, id: Long) {
        viewModel.post { controller ->
            val controls = controller.transportControls
            val clickedItem = songAdapter.getItem(position)
            controls.playFromMediaId(clickedItem.mediaId, null)
        }
    }

    private fun showDeleteDialog() {
        val checkedItemCount = list.checkedItemCount
        val dialogMessage = resources.getQuantityString(
            R.plurals.delete_dialog_message,
            checkedItemCount, checkedItemCount
        )

        val confirm = ConfirmDialogFragment.newInstance(
            this, R.id.request_delete_tracks,
            getString(R.string.delete_dialog_title), dialogMessage,
            R.string.action_delete, R.string.cancel, 0
        )
        confirm.show(fragmentManager!!, null)
    }

    private fun deleteSelectedTracks() {
        val checkedItemIds = list.checkedItemIds
        val params = Bundle(1)
        params.putLongArray(DeleteTracksCommand.PARAM_TRACK_IDS, checkedItemIds)

        viewModel.postCommand(DeleteTracksCommand.CMD_NAME, params) { resultCode, _ ->
            val rootView = view
            if (resultCode == R.id.result_success && rootView != null) {
                val userMessage = resources.getQuantityString(
                    R.plurals.deleted_songs_confirmation,
                    checkedItemIds.size
                )
                Toast.makeText(context, userMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openPlaylistChooserDialog() {
        val checkedIds = list.checkedItemIds
        val dialog = AddToPlaylistDialog.newInstance(this, R.id.request_add_to_playlist, checkedIds)
        dialog.show(fragmentManager!!, AddToPlaylistDialog.TAG)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        // User has confirmed his intent to delete track(s)
        if (requestCode == R.id.request_delete_tracks && resultCode == DialogInterface.BUTTON_POSITIVE) {
            deleteSelectedTracks()
            multiSelectMode.finish()
        }

        else if (requestCode == R.id.request_add_to_playlist) {

            when (resultCode) {
                Activity.RESULT_OK -> {
                    // Track(s) have been added to a playlist
                    if (data != null) {
                        // Display a confirmation message as a Toast
                        val count = data.getIntExtra(AddToPlaylistDialog.RESULT_TRACK_COUNT, 0)
                        val title = data.getStringExtra(AddToPlaylistDialog.RESULT_PLAYLIST_TITLE)

                        val userMessage = resources.getQuantityString(
                            R.plurals.tracks_added_to_playlist, count, count, title)
                        Toast.makeText(context, userMessage, Toast.LENGTH_SHORT).show()
                    }

                    // Finish action mode to deselect all items
                    multiSelectMode.finish()
                }

                R.id.error_playlist_already_exists -> {
                    // Failed to insert to a playlist due to its name being already taken
                    data ?: throw IllegalStateException("Dialog should send information back")
                    val title = data.getStringExtra(NewPlaylistDialog.RESULT_TAKEN_PLAYLIST_TITLE)

                    val userMessage = getString(R.string.error_playlist_title_taken, title)
                    Toast.makeText(context, userMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * An ActionMode that handles multiple item selection inside the song ListView.
     */
    private inner class SongListActionMode : MultiChoiceModeListener {
        private var actionMode: ActionMode? = null

        override fun onItemCheckedStateChanged(mode: ActionMode, position: Int,
                                               id: Long, checked: Boolean) {
            mode.title = list.checkedItemCount.toString()
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.actionmode_songlist, menu)
            actionMode = mode
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem) = when (item.itemId) {
            R.id.action_delete -> {
                showDeleteDialog()
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

    companion object Factory {
        fun newInstance(): SongListFragment {
            val args = Bundle(1)
            args.putInt(Constants.FRAGMENT_ID, R.id.action_all)
            val fragment = SongListFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
