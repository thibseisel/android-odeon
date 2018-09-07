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
import android.support.v4.media.MediaMetadataCompat
import fr.nihilus.music.media.R
import fr.nihilus.music.media.asMediaDescription
import fr.nihilus.music.media.CATEGORY_ARTISTS
import fr.nihilus.music.media.browseHierarchyOf
import fr.nihilus.music.media.source.MusicDao
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject

internal class ArtistItems
@Inject constructor(
    private val context: Context,
    private val musicDao: MusicDao
) : BuiltinItem {

    override fun asMediaItem(): Single<MediaItem> {
        val description = MediaDescriptionCompat.Builder()
            .setMediaId(CATEGORY_ARTISTS)
            .setTitle(context.getString(R.string.abc_artists))
            .build()
        val item = MediaItem(description, MediaItem.FLAG_BROWSABLE)
        return Single.just(item)
    }

    override fun getChildren(parentMediaId: String): Observable<MediaItem> {
        val hierarchy = browseHierarchyOf(parentMediaId)
        return if (hierarchy.size > 1) {
            val artistId = hierarchy[1]
            fetchArtistChildren(artistId)
        } else {
            fetchAllArtists()
        }
    }

    private fun fetchAllArtists(): Observable<MediaItem> =
        musicDao.getArtists().map { MediaItem(it, MediaItem.FLAG_BROWSABLE) }

    private fun fetchArtistChildren(artistId: String): Observable<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()

        val criteria = mapOf(MusicDao.METADATA_KEY_ARTIST_ID to artistId)
        val albumSorting = "${MediaMetadataCompat.METADATA_KEY_YEAR} DESC"

        val albums = musicDao.getAlbums(criteria, albumSorting)
            .map { MediaItem(it, MediaItem.FLAG_BROWSABLE or MediaItem.FLAG_BROWSABLE) }

        val tracks = musicDao.getTracks(criteria, null)
            .map {
                val description = it.asMediaDescription(builder, CATEGORY_ARTISTS, artistId)
                MediaItem(description, MediaItem.FLAG_PLAYABLE)
            }
        return Observable.concat(albums, tracks)
    }
}