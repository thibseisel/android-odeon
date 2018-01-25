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

package fr.nihilus.music.ui.songs

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.support.v4.app.Fragment
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback
import android.support.v4.view.ViewCompat
import android.support.v7.widget.SearchView
import android.view.*
import android.widget.AbsListView.MultiChoiceModeListener
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import dagger.android.support.AndroidSupportInjection
import fr.nihilus.music.Constants
import fr.nihilus.music.R
import fr.nihilus.music.client.BrowserViewModel
import fr.nihilus.music.command.DeleteTracksCommand
import fr.nihilus.music.command.MediaSessionCommand
import fr.nihilus.music.utils.ConfirmDialogFragment
import fr.nihilus.music.utils.MediaID
import kotlinx.android.synthetic.main.fragment_songs.*

class SongListFragment : Fragment(),
    AdapterView.OnItemClickListener {

    private val multiSelectMode = SongListActionMode()

    private lateinit var songAdapter: SongAdapter
    private lateinit var viewModel: BrowserViewModel

    private val subscriptionCallback = object : SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, items: List<MediaItem>) {
            songAdapter.updateItems(items)
            progress.hide()
            listContainer.visibility = View.VISIBLE
        }
    }

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
        val searchView = menu.findItem(R.id.action_search).actionView as SearchView
        // TODO Search and filtering features
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_play_shuffled -> {
            viewModel.post { controller ->
                controller.transportControls.playFromMediaId(MediaID.ID_MUSIC, Bundle(1).apply {
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
    ): View =
        inflater.inflate(R.layout.fragment_songs, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(list) {
            adapter = songAdapter
            onItemClickListener = this@SongListFragment
            emptyView = view.findViewById(android.R.id.empty)
            choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
            setMultiChoiceModeListener(multiSelectMode)
            ViewCompat.setNestedScrollingEnabled(this, true)
        }

        listContainer.visibility = View.GONE
        if (savedInstanceState == null) {
            progress.show()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(activity!!).get(BrowserViewModel::class.java)
    }

    override fun onStart() {
        super.onStart()
        activity!!.setTitle(R.string.all_music)
        viewModel.subscribe(MediaID.ID_MUSIC, subscriptionCallback)
    }

    override fun onStop() {
        viewModel.unsubscribe(MediaID.ID_MUSIC)
        super.onStop()
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
            this, 21,
            getString(R.string.delete_dialog_title), dialogMessage,
            R.string.action_delete, R.string.cancel, 0
        )
        confirm.show(fragmentManager!!, null)
    }

    private fun deleteSelectedTracks() {
        var index = 0
        val checkedPositions = list.checkedItemPositions
        val toDelete = LongArray(list.checkedItemCount)
        for (i in 0 until checkedPositions.size()) {
            if (checkedPositions.valueAt(i)) {
                val pos = checkedPositions.keyAt(i)
                toDelete[index++] = songAdapter.getItemId(pos)
            }
        }

        val params = Bundle(1)
        params.putLongArray(DeleteTracksCommand.PARAM_TRACK_IDS, toDelete)

        viewModel.post { controller ->
            controller.sendCommand(DeleteTracksCommand.CMD_NAME,
                params, object : ResultReceiver(Handler()) {

                    override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                        val rootView = view
                        if (resultCode == MediaSessionCommand.CODE_SUCCESS && rootView != null) {
                            val message = resources
                                .getQuantityString(
                                    R.plurals.deleted_songs_confirmation,
                                    toDelete.size
                                )
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    }
                })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_DELETE_TRACKS && resultCode == DialogInterface.BUTTON_POSITIVE) {
            deleteSelectedTracks()
            multiSelectMode.finish()
        }
    }

    /**
     * An ActionMode that handles multiple item selection inside the song ListView.
     */
    private inner class SongListActionMode : MultiChoiceModeListener {
        private var actionMode: ActionMode? = null

        override fun onItemCheckedStateChanged(
            mode: ActionMode,
            position: Int,
            id: Long,
            checked: Boolean
        ) {
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
                // TODO Prepare a playlist with the selected items
                mode.finish()
                true
            }
            else -> false
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            // By default, deselect items
            actionMode = null
        }

        fun finish() {
            actionMode?.finish()
        }
    }

    companion object Factory {

        private const val REQUEST_CODE_DELETE_TRACKS = 21

        fun newInstance(): SongListFragment {
            val args = Bundle(1)
            args.putInt(Constants.FRAGMENT_ID, R.id.action_all)
            val fragment = SongListFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
