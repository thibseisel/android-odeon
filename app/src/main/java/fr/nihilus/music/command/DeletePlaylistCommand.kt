package fr.nihilus.music.command

import android.os.Bundle
import android.os.ResultReceiver
import android.util.Log
import fr.nihilus.music.database.PlaylistDao
import fr.nihilus.music.di.ServiceScoped
import fr.nihilus.music.service.MusicService
import fr.nihilus.music.utils.MediaID
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

@ServiceScoped
class DeletePlaylistCommand
@Inject internal constructor(
        private val service: MusicService,
        private val playlistDao: PlaylistDao
) : MediaSessionCommand {

    override fun handle(params: Bundle?, cb: ResultReceiver?) {
        params ?: throw IllegalArgumentException("This command should have parameters")
        val playlistId = params.getLong(PARAM_PLAYLIST_ID, -1L)
        require (playlistId != -1L) { "Missing parameter: PARAM_PLAYLIST_ID" }

        Single.fromCallable { playlistDao.deletePlaylist(playlistId) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({
                    service.notifyChildrenChanged(MediaID.ID_PLAYLISTS)
                    cb?.send(MediaSessionCommand.CODE_SUCCESS, null)
                }, { error ->
                    Log.e(TAG, "Unexpected error while deleting playlist", error)
                    cb?.send(MediaSessionCommand.CODE_UNEXPECTED_ERROR, null)
                })
    }

    companion object {
        private const val TAG = "DeletePlaylistCmd"

        /**
         * The name of this command.
         *
         * Required parameters:
         * - [PARAM_PLAYLIST_ID]
         */
        const val CMD_NAME = "fr.nihilus.music.command.DeletePlaylistCommand"
        const val PARAM_PLAYLIST_ID = "playlist_id"
    }
}