package fr.nihilus.music.command

import android.os.Bundle
import android.os.ResultReceiver
import fr.nihilus.music.database.Playlist
import fr.nihilus.music.database.PlaylistDao
import fr.nihilus.music.database.PlaylistTrack
import fr.nihilus.music.di.ServiceScoped
import fr.nihilus.music.service.MusicService
import fr.nihilus.music.utils.MediaID
import io.reactivex.Single
import javax.inject.Inject

@ServiceScoped
class NewPlaylistCommand
@Inject internal constructor(
        private val service: MusicService,
        private val playlistDao: PlaylistDao
) : MediaSessionCommand {

    override fun handle(params: Bundle?, cb: ResultReceiver?) {
        params ?: throw IllegalArgumentException("This command has parameters.")
        val playlistTitle = params.getString(PARAM_TITLE) ?:
                throw IllegalArgumentException("Missing parameter: PARAM_TITLE")
        val playlist = Playlist.create(playlistTitle)
        val trackIds = params.getLongArray(PARAM_TRACK_IDS) ?: longArrayOf()

        Single.fromCallable { savePlaylist(playlist) }
                .doOnSuccess { service.notifyChildrenChanged(MediaID.ID_PLAYLISTS) }
                .map { playlistId ->
                    trackIds.map { musicId ->
                        PlaylistTrack(playlistId, musicId)
                    }
                }
                .doOnSuccess { playlistDao.addTracks(it) }
                .subscribe(
                        { _ -> onSuccess(cb) },
                        { error -> onError(error, cb) }
                )
    }

    private fun savePlaylist(playlist: Playlist): Long {
        return playlistDao.savePlaylist(playlist).also {
            if (it == -1L) {
                throw IllegalArgumentException("A playlist with this title already exists.")
            }
        }
    }

    private fun onSuccess(cb: ResultReceiver?) {
        cb?.send(CODE_SUCCESS, null)
    }

    private fun onError(error: Throwable?, cb: ResultReceiver?) {
        if (error is IllegalArgumentException) {
            cb?.send(CODE_ERROR_TITLE_ALREADY_EXISTS, null)
        } else {
            cb?.send(CODE_UNKNWON_ERROR, null)
        }
    }

    companion object {

        /**
         * The name of this command.
         */
        const val CMD_NAME = "new_playlist"

        /**
         * The title of the new playlist to create, as a String value.
         *
         * If this name is already taken, [CODE_ERROR_TITLE_ALREADY_EXISTS]
         * will be returned.
         */
        const val PARAM_TITLE = "playlist_title"

        /**
         * An array of Long containing all music ids of tracks
         * to add to the newly created playlist.
         *
         * If not specified, no track will be added to the playlist.
         */
        const val PARAM_TRACK_IDS = "track_ids"

        const val CODE_SUCCESS = 0
        const val CODE_UNKNWON_ERROR = -1
        const val CODE_ERROR_TITLE_ALREADY_EXISTS = -2
    }
}