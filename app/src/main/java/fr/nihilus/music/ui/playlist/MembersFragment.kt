package fr.nihilus.music.ui.playlist

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import dagger.android.support.AndroidSupportInjection
import fr.nihilus.music.R
import fr.nihilus.music.di.ActivityScoped
import fr.nihilus.music.library.NavigationController
import fr.nihilus.music.playlists.PlaylistRepository
import fr.nihilus.music.utils.MediaID
import fr.nihilus.recyclerfragment.RecyclerFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

@ActivityScoped
class MembersFragment : RecyclerFragment() {

    private lateinit var mAdapter: MembersAdapter
    private lateinit var mPlaylist: MediaBrowserCompat.MediaItem

    @Inject lateinit var mRouter: NavigationController
    @Inject lateinit var mRepository: PlaylistRepository

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        mAdapter = MembersAdapter(this)
        mPlaylist = arguments.getParcelable(ARG_PLAYLIST)
                ?: throw IllegalStateException("Fragment must be instantiated with newInstance")
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_playlist_details, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete -> {
                deleteThisPlaylist()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        adapter = mAdapter
        if (savedInstanceState == null) {
            //setRecyclerShown(false);
        }
    }

    override fun onStart() {
        super.onStart()
        activity.title = mPlaylist.description.title
    }

    private fun deleteThisPlaylist() {
        val playlistId = MediaID.extractBrowseCategoryValueFromMediaID(mPlaylist.mediaId!!)
        mRepository.deletePlaylist(playlistId.toLong())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { mRouter.navigateBack() }
    }

    companion object {
        private const val ARG_PLAYLIST = "playlist"

        @JvmStatic fun newInstance(playlist: MediaBrowserCompat.MediaItem): MembersFragment {
            val args = Bundle(1)
            args.putParcelable(ARG_PLAYLIST, playlist)
            val fragment = MembersFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
