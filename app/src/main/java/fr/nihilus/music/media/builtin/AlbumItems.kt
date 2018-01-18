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
import fr.nihilus.music.media.source.MusicDao
import fr.nihilus.music.utils.MediaID
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject

internal class AlbumItems
@Inject constructor(
    private val context: Context,
    private val musicDao: MusicDao
) : BuiltinItem {

    override fun asMediaItem(): Single<MediaItem> {
        val description = MediaDescriptionCompat.Builder()
            .setMediaId(MediaID.ID_ALBUMS)
            .setTitle(context.getString(R.string.action_albums))
            .build()
        return Single.just(MediaItem(description, MediaItem.FLAG_BROWSABLE))
    }

    override fun getChildren(parentMediaId: String): Observable<MediaItem> {
        val hierarchy = MediaID.getHierarchy(parentMediaId)
        return if (hierarchy.size > 1) {
            val albumId = hierarchy[1]
            fetchAlbumTracks(albumId)
        } else {
            fetchAllAlbums()
        }
    }

    private fun fetchAllAlbums(): Observable<MediaItem> {
        return musicDao.getAlbums(null, null)
            .map { MediaItem(it, MediaItem.FLAG_BROWSABLE or MediaItem.FLAG_PLAYABLE) }
    }

    private fun fetchAlbumTracks(albumId: String): Observable<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()
        return musicDao.getTracks(
            mapOf(MusicDao.METADATA_KEY_ALBUM_ID to albumId.toLong()),
            MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER
        )
            .map {
                val description = it.asMediaDescription(builder, MediaID.ID_ALBUMS, albumId)
                MediaItem(description, MediaItem.FLAG_PLAYABLE)
            }
    }
}