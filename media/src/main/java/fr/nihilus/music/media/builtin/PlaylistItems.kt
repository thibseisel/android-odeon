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

package fr.nihilus.music.media.builtin

import android.content.Context
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import fr.nihilus.music.media.R
import fr.nihilus.music.media.asMediaDescription
import fr.nihilus.music.media.database.PlaylistDao
import fr.nihilus.music.media.CATEGORY_PLAYLISTS
import fr.nihilus.music.media.browseHierarchyOf
import fr.nihilus.music.media.source.MusicDao
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

internal class PlaylistItems
@Inject constructor(
    private val context: Context,
    private val playlistDao: PlaylistDao,
    private val musicDao: MusicDao,
    private val mostRecentTracks: MostRecentTracks
) : BuiltinItem {

    override fun asMediaItem(): Single<MediaItem> {
        val description = MediaDescriptionCompat.Builder()
            .setMediaId(CATEGORY_PLAYLISTS)
            .setTitle(context.getString(R.string.abc_playlists))
            .build()
        return Single.just(MediaItem(description, MediaItem.FLAG_BROWSABLE))
    }

    override fun getChildren(parentMediaId: String): Observable<MediaItem> {
        val hierarchy = browseHierarchyOf(parentMediaId)
        return if (hierarchy.size > 1) {
            val playlistId = hierarchy[1]
            fetchPlaylistMembers(playlistId)
        } else {
            Observable.concat(fetchBuiltInPlaylists(), fetchUserPlaylists())
        }
    }

    private fun fetchBuiltInPlaylists(): Observable<MediaItem> {
        // As the number of predefined playlists grow, use Single.concat(item1, item2...)
        return mostRecentTracks.asMediaItem().toObservable()
    }

    private fun fetchUserPlaylists(): Observable<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()
        return playlistDao.getPlaylists()
            .flatMapObservable { Observable.fromIterable(it) }
            .map { playlist ->
                val description = playlist.asMediaDescription(builder)
                MediaItem(description, MediaItem.FLAG_BROWSABLE or MediaItem.FLAG_PLAYABLE)
            }
    }

    private fun fetchPlaylistMembers(playlistId: String): Observable<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()
        return playlistDao.getPlaylistTracks(playlistId.toLong())
            .subscribeOn(Schedulers.io())
            .flatMapObservable { Observable.fromIterable(it) }
            .flatMapMaybe { musicDao.findTrack(it.musicId.toString()) }
            .map { member ->
                val descr = member.asMediaDescription(builder, CATEGORY_PLAYLISTS, playlistId)
                MediaItem(descr, MediaItem.FLAG_PLAYABLE)
            }
    }

}