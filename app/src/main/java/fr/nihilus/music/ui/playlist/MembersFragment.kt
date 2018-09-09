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

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import dagger.android.support.AndroidSupportInjection
import fr.nihilus.music.R
import fr.nihilus.music.client.BrowserViewModel
import fr.nihilus.music.client.NavigationController
import fr.nihilus.music.di.ActivityScoped
import fr.nihilus.music.media.Constants
import fr.nihilus.music.media.command.DeletePlaylistCommand
import fr.nihilus.music.media.utils.MediaID
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.utils.ConfirmDialogFragment
import fr.nihilus.recyclerfragment.RecyclerFragment
import timber.log.Timber
import javax.inject.Inject

@ActivityScoped
class MembersFragment : RecyclerFragment(), BaseAdapter.OnItemSelectedListener {

    private lateinit var adapter: MembersAdapter
    private lateinit var playlist: MediaItem
    private lateinit var viewModel: BrowserViewModel

    @Inject lateinit var router: NavigationController

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        adapter = MembersAdapter(this, this)

        playlist = arguments?.getParcelable(ARG_PLAYLIST)
                ?: throw IllegalStateException("Fragment must be instantiated with newInstance")
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_playlist_details, menu)
        menu.findItem(R.id.action_delete).isVisible = arguments!!.getBoolean(ARG_DELETABLE)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete -> {
                val dialogTitle = getString(
                    R.string.delete_playlist_dialog_title,
                    playlist.description.title
                )
                ConfirmDialogFragment.newInstance(
                    this, REQUEST_DELETE_PLAYLIST,
                    title = dialogTitle,
                    positiveButton = R.string.ok,
                    negativeButton = R.string.cancel
                ).show(fragmentManager, null)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(activity!!).get(BrowserViewModel::class.java)
        viewModel.subscribeTo(playlist.mediaId!!).observe(this, Observer {
            adapter.submitList(it.orEmpty())
            setRecyclerShown(true)
        })

        setAdapter(adapter)
        recyclerView.setHasFixedSize(true)

        if (savedInstanceState == null) {
            setRecyclerShown(false)
        }
    }

    override fun onStart() {
        super.onStart()
        activity!!.title = playlist.description.title
    }

    override fun onItemSelected(position: Int, actionId: Int) {
        val member = adapter.getItem(position)
        viewModel.post { controller ->
            controller.transportControls.playFromMediaId(member.mediaId, null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_DELETE_PLAYLIST && resultCode == DialogInterface.BUTTON_POSITIVE) {
            deleteThisPlaylist()
        }
    }

    private fun deleteThisPlaylist() {
        val playlistId = MediaID.categoryValueOf(playlist.mediaId!!).toLong()
        val params = Bundle(1).apply {
            putLong(DeletePlaylistCommand.PARAM_PLAYLIST_ID, playlistId)
        }

        viewModel.postCommand(DeletePlaylistCommand.CMD_NAME, params) { resultCode, _ ->
            if (resultCode == R.id.result_success) router.navigateBack()
            else Timber.e("Delete playlist: unexpected resultCode = %d", resultCode)
        }
    }

    companion object Factory {
        private const val ARG_PLAYLIST = "playlist"
        private const val ARG_DELETABLE = "deletable"
        private const val REQUEST_DELETE_PLAYLIST = 66

        fun newInstance(playlist: MediaItem, deletable: Boolean) = MembersFragment().apply {
            arguments = Bundle(3).apply {
                putInt(Constants.FRAGMENT_ID, R.id.action_playlist)
                putParcelable(ARG_PLAYLIST, playlist)
                putBoolean(ARG_DELETABLE, deletable)
            }
        }
    }
}
