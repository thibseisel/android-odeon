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
import fr.nihilus.music.utils.loadResourceAsBitmap
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject

/**
 * A built-in item that groups tracks that have been added to the music library recently.
 */
internal class MostRecentTracks
@Inject constructor(
        private val context: Context,
        private val dao: MusicDao
) : BuiltinItem {

    override fun asMediaItem(): Single<MediaItem> {
        val recentIcon = loadResourceAsBitmap(context, R.drawable.ic_most_recent_128dp, 320, 320)
        val builder = MediaDescriptionCompat.Builder()
        val description = builder
                .setMediaId(MediaID.ID_MOST_RECENT)
                .setTitle(context.getText(R.string.last_added))
                .setIconBitmap(recentIcon)
                .build()
        val item = MediaItem(description, MediaItem.FLAG_PLAYABLE or MediaItem.FLAG_BROWSABLE)
        return Single.just(item)
    }

    override fun getChildren(parentMediaId: String): Observable<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()
        return dao.getTracks(null, "${MusicDao.METADATA_KEY_DATE} DESC").take(50)
                .map {
                    val description = it.asMediaDescription(builder, MediaID.ID_MOST_RECENT)
                    MediaItem(description, MediaItem.FLAG_PLAYABLE)
                }
    }
}