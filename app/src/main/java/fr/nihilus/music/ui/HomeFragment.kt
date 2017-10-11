package fr.nihilus.music.ui

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v7.widget.GridLayoutManager
import dagger.android.support.AndroidSupportInjection
import fr.nihilus.music.R
import fr.nihilus.music.di.ActivityScoped
import fr.nihilus.music.library.BrowserViewModel
import fr.nihilus.music.library.NavigationController
import fr.nihilus.music.ui.playlist.PlaylistsAdapter
import fr.nihilus.music.utils.MediaID
import fr.nihilus.recyclerfragment.RecyclerFragment
import javax.inject.Inject

@ActivityScoped
class HomeFragment : RecyclerFragment(), PlaylistsAdapter.OnPlaylistSelectedListener {

    private  lateinit var mAdapter: PlaylistsAdapter
    private lateinit var mViewModel: BrowserViewModel

    @Inject lateinit var mRouter: NavigationController

    private val mSubscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaItem>) {
            mAdapter.update(children)
            setRecyclerShown(true)
        }
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAdapter = PlaylistsAdapter(this)
        mAdapter.setOnPlaylistSelectedListener(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mViewModel = ViewModelProviders.of(activity).get(BrowserViewModel::class.java)
        val spanCount = resources.getInteger(R.integer.album_grid_span_count)
        recyclerView.layoutManager = GridLayoutManager(context, spanCount)

        adapter = mAdapter
        if (savedInstanceState == null) {
            setRecyclerShown(false)
        }
    }

    override fun onStart() {
        super.onStart()
        activity.setTitle(R.string.home)
        mViewModel.subscribe(MediaID.ID_AUTO, mSubscriptionCallback)
    }

    override fun onStop() {
        mViewModel.unsubscribe(MediaID.ID_AUTO)
        super.onStop()
    }

    override fun onPlaylistSelected(holder: PlaylistsAdapter.PlaylistHolder, playlist: MediaItem) {
        mRouter.navigateToPlaylistDetails(playlist)
    }

    override fun onPlay(playlist: MediaItem) {
        mViewModel.playFromMediaId(playlist.mediaId!!)
    }
}