package fr.nihilus.music.command

import android.os.Bundle
import android.os.ResultReceiver
import android.util.Log
import fr.nihilus.music.di.ServiceScoped
import fr.nihilus.music.media.source.MusicDao
import fr.nihilus.music.service.MusicService
import fr.nihilus.music.utils.MediaID
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

@ServiceScoped
class DeleteTracksCommand
@Inject internal constructor(
        private val service: MusicService,
        private val musicDao: MusicDao
) : MediaSessionCommand {

    override fun handle(params: Bundle?, cb: ResultReceiver?) {
        val idsToDelete = params?.getLongArray(PARAM_TRACK_IDS)
                ?: throw IllegalArgumentException("Required parameter: PARAM_TRACK_IDS")

        Observable.fromIterable(idsToDelete.toSet())
                .subscribeOn(Schedulers.io())
                .flatMapCompletable { musicId -> musicDao.deleteTrack(musicId.toString()) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { onSuccess(cb) },
                        { error -> onError(error, cb) })
    }

    private fun onSuccess(cb: ResultReceiver?) {
        service.notifyChildrenChanged(MediaID.ID_MUSIC)
        cb?.send(MediaSessionCommand.CODE_SUCCESS, null)
    }

    private fun onError(error: Throwable, cb: ResultReceiver?) {
        Log.e(TAG, "Unexpected error while deleting tracks", error)
        cb?.send(MediaSessionCommand.CODE_UNEXPECTED_ERROR, null)
    }

    companion object {

        private const val TAG = "DeleteTracksCmd"

        /**
         * The name of this command.
         *
         * Required parameters:
         * - [PARAM_TRACK_IDS]
         */
        const val CMD_NAME = "fr.nihilus.music.command.DeleteTrackCommand"

        /**
         * An array containing the music ids of the tracks to delete.
         *
         * Type: long array
         */
        const val PARAM_TRACK_IDS = "track_ids"
    }
}