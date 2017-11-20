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
import fr.nihilus.music.R
import fr.nihilus.music.asMediaDescription
import fr.nihilus.music.media.source.MusicDao
import fr.nihilus.music.utils.MediaID
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject

internal class ArtistItems
@Inject constructor(
        private val context: Context,
        private val musicDao: MusicDao
) : BuiltinItem {

    override fun asMediaItem(): MediaItem {
        val description = MediaDescriptionCompat.Builder()
                .setMediaId(MediaID.ID_ARTISTS)
                .setTitle(context.getString(R.string.action_artists))
                .build()
        return MediaItem(description, MediaItem.FLAG_BROWSABLE)
    }

    override fun getChildren(parentMediaId: String): Single<List<MediaItem>> {
        val hierarchy = MediaID.getHierarchy(parentMediaId)
        return if (hierarchy.size > 1) {
            val artistId = hierarchy[1]
            fetchArtistChildren(artistId)
        } else {
            fetchAllArtists()
        }
    }

    private fun fetchAllArtists(): Single<List<MediaItem>> {
        return musicDao.getArtists().flatMap { Observable.fromIterable(it) }
                .map { MediaItem(it, MediaItem.FLAG_BROWSABLE or MediaItem.FLAG_PLAYABLE) }
                .toList()
    }

    private fun fetchArtistChildren(artistId: String): Single<List<MediaItem>> {
        val builder = MediaDescriptionCompat.Builder()
        val albums = musicDao.getArtistAlbums(artistId)
                .flatMap { Observable.fromIterable(it) }
                .map { MediaItem(it, MediaItem.FLAG_BROWSABLE or MediaItem.FLAG_BROWSABLE) }
        val tracks = musicDao.getArtistTracks(artistId)
                .flatMap { Observable.fromIterable(it) }
                .map { it.asMediaDescription(builder, MediaID.ID_ARTISTS, artistId) }
                .map { MediaItem(it, MediaItem.FLAG_PLAYABLE) }
        return Observable.concat(albums, tracks).toList()
    }
}