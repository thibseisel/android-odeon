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

package fr.nihilus.music.media.builtin

import android.content.Context
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import fr.nihilus.music.R
import fr.nihilus.music.asMediaDescription
import fr.nihilus.music.database.PlaylistDao
import fr.nihilus.music.media.source.MusicDao
import fr.nihilus.music.utils.MediaID
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import javax.inject.Inject

internal class PlaylistItems
@Inject constructor(
        private val context: Context,
        private val playlistDao: PlaylistDao,
        private val musicDao: MusicDao,
        private val random: AllTracksRandom,
        private val mostRecentTracks: MostRecentTracks
) : BuiltinItem {

    override fun asMediaItem(): MediaItem {
        val description = MediaDescriptionCompat.Builder()
                .setMediaId(MediaID.ID_PLAYLISTS)
                .setTitle(context.getString(R.string.action_playlists))
                .build()
        return MediaItem(description, MediaItem.FLAG_BROWSABLE)
    }

    override fun getChildren(parentMediaId: String): Single<List<MediaItem>> {
        val hierarchy = MediaID.getHierarchy(parentMediaId)
        return if (hierarchy.size > 1) {
            val playlistId = hierarchy[1]
            fetchPlaylistMembers(playlistId)
        } else {
            Single.zip(fetchBuiltInPlaylists(), fetchUserPlaylists(), BiFunction { builtin, userDefined ->
                ArrayList<MediaItem>(builtin.size + userDefined.size).apply {
                    addAll(builtin)
                    addAll(userDefined)
                }
            })
        }
    }

    private fun fetchBuiltInPlaylists(): Single<List<MediaItem>> {
        return Single.fromCallable {
            listOf(
                    mostRecentTracks.asMediaItem(),
                    random.asMediaItem()
            )
        }
    }

    private fun fetchUserPlaylists(): Single<List<MediaItem>> {
        val builder = MediaDescriptionCompat.Builder()
        return playlistDao.playlists.take(1)
                .flatMap { Flowable.fromIterable(it) }
                .map { playlist ->
                    val description = playlist.asMediaDescription(builder)
                    MediaItem(description, MediaItem.FLAG_BROWSABLE or MediaItem.FLAG_PLAYABLE)
                }.toList()
    }

    private fun fetchPlaylistMembers(playlistId: String): Single<List<MediaItem>> {
        val builder = MediaDescriptionCompat.Builder()
        return playlistDao.getPlaylistTracks(playlistId.toLong()).take(1)
                .flatMap { Flowable.fromIterable(it) }
                .flatMapSingle { fetchMetadata(it.musicId.toString()) }
                .map { member ->
                    val descr = member.asMediaDescription(builder, MediaID.ID_PLAYLISTS, playlistId)
                    MediaItem(descr, MediaItem.FLAG_PLAYABLE)
                }.toList()
    }

    private fun fetchMetadata(musicId: String): Single<MediaMetadataCompat> =
            musicDao.findTrack(musicId).toSingle()

}