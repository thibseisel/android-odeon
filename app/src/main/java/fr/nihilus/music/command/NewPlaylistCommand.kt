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

package fr.nihilus.music.command

import android.database.sqlite.SQLiteConstraintException
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.util.Log
import com.github.thibseisel.kdenticon.Identicon
import com.github.thibseisel.kdenticon.IdenticonStyle
import com.github.thibseisel.kdenticon.android.drawToBitmap
import fr.nihilus.music.BuildConfig
import fr.nihilus.music.database.Playlist
import fr.nihilus.music.database.PlaylistDao
import fr.nihilus.music.database.PlaylistTrack
import fr.nihilus.music.di.ServiceScoped
import fr.nihilus.music.service.MusicService
import fr.nihilus.music.utils.MediaID
import fr.nihilus.music.utils.PermissionUtil
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.util.*
import javax.inject.Inject

/**
 * A custom command that adds a new user-specified playlist to the music library.
 */
@ServiceScoped
class NewPlaylistCommand
@Inject internal constructor(
    private val service: MusicService,
    private val playlistDao: PlaylistDao
) : MediaSessionCommand {

    override fun handle(params: Bundle?, cb: ResultReceiver?) {
        params ?: throw IllegalArgumentException("This command should have parameters")
        val playlistTitle = params.getString(PARAM_TITLE)
                ?: throw IllegalArgumentException("Missing parameter: PARAM_TITLE")
        val trackIds = params.getLongArray(PARAM_TRACK_IDS) ?: LongArray(0)

        val playlist = Playlist().apply {
            title = playlistTitle
            artUri = generateIcon(playlistTitle)
            created = Date()
        }

        Single.fromCallable { savePlaylist(playlist) }
            .subscribeOn(Schedulers.io())
            .doOnSuccess { service.notifyChildrenChanged(MediaID.ID_PLAYLISTS) }
            .map { playlistId ->
                trackIds.map { musicId ->
                    PlaylistTrack(playlistId, musicId)
                }
            }
            .doOnSuccess { playlistDao.addTracks(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { onSuccess(cb) },
                { error ->
                    Log.i(TAG, "An error occurred while creating playlist", error)
                    onError(error, cb)
                }
            )
    }

    private fun savePlaylist(playlist: Playlist): Long {
        return playlistDao.savePlaylist(playlist).also {
            if (it == -1L) {
                throw IllegalStateException()
            }
        }
    }

    private fun onSuccess(cb: ResultReceiver?) {
        cb?.send(MediaSessionCommand.CODE_SUCCESS, null)
    }

    private fun onError(error: Throwable, cb: ResultReceiver?) {
        if (error is SQLiteConstraintException) {
            cb?.send(CODE_ERROR_TITLE_ALREADY_EXISTS, null)
        } else if (BuildConfig.DEBUG) {
            // Rethrow unexpected errors on debug builds
            throw error
        }
    }

    private fun generateIcon(seed: CharSequence): Uri {
        if (PermissionUtil.hasExternalStoragePermission(service)) {

            val bitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)
            Identicon.fromValue(seed, 320).apply {
                style = IdenticonStyle(
                    backgroundColor = Color.TRANSPARENT,
                    padding = 0f
                )
            }.drawToBitmap(bitmap)

            // Create the directory that stores generated identicons in internal storage
            val iconsDirectory = File(service.filesDir, "identicons")
            if (!iconsDirectory.exists()) {
                iconsDirectory.mkdir()
            }

            // Remove spaces from the seed to make a filename without spaces
            val fileName = seed.replace("\\s".toRegex(), "_")
            val iconFile = File(iconsDirectory, "$fileName.png")

            val successful = iconFile.outputStream().use { os ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, os)
            }

            return if (successful) Uri.fromFile(iconFile) else Uri.EMPTY
        }

        Log.i(TAG, "Cannot create icon: no permission to write to external storage.")
        return Uri.EMPTY
    }

    companion object {
        private const val TAG = "NewPlaylistCmd"

        /**
         * The name of this command.
         *
         * Required parameters:
         * - [PARAM_TITLE]
         * - [PARAM_TRACK_IDS]
         */
        const val CMD_NAME = "fr.nihilus.music.command.NewPlaylistCommand"

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

        /**
         * An error code that denotes that the attempt to create a playlist has failed
         * because a playlist with that title already exists.
         */
        const val CODE_ERROR_TITLE_ALREADY_EXISTS = -2
    }
}