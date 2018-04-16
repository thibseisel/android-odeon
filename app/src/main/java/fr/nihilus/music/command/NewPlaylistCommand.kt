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

package fr.nihilus.music.command

import android.database.sqlite.SQLiteConstraintException
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import com.github.thibseisel.kdenticon.Identicon
import com.github.thibseisel.kdenticon.IdenticonStyle
import com.github.thibseisel.kdenticon.android.drawToBitmap
import fr.nihilus.music.R
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
import timber.log.Timber
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
        cb?.send(R.id.result_success, null)
    }

    private fun onError(error: Throwable, cb: ResultReceiver?) {
        // TODO Maybe the unique constraint on the title could be removed
        if (error is SQLiteConstraintException) {
            cb?.send(R.id.error_playlist_already_exists, null)
        } else {
            Timber.w(error, "An error occurred while creating playlist")
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

        Timber.w("Cannot create icon: no permission to write to external storage.")
        return Uri.EMPTY
    }

    companion object {

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
         * If this name is already taken, the code [R.id.error_playlist_already_exists]
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
    }
}