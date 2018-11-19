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
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import fr.nihilus.music.media.CATEGORY_MOST_RATED
import fr.nihilus.music.media.R
import fr.nihilus.music.media.asMediaDescription
import fr.nihilus.music.media.source.MusicDao
import fr.nihilus.music.media.usage.MediaUsageManager
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject

internal class MostRatedTracks
@Inject constructor(
    private val context: Context,
    private val mediaDao: MusicDao,
    private val usageManager: MediaUsageManager
) : BuiltinItem {

    override fun asMediaItem(): Single<MediaBrowserCompat.MediaItem> {
        val builder = MediaDescriptionCompat.Builder()
        val description = builder.setMediaId(CATEGORY_MOST_RATED)
            .setTitle(context.getString(R.string.abc_most_rated))
            .build()
        // TODO Icon
        val item = MediaItem(description, MediaItem.FLAG_BROWSABLE or MediaItem.FLAG_PLAYABLE)
        return Single.just(item)
    }

    override fun getChildren(parentMediaId: String): Observable<MediaBrowserCompat.MediaItem> {
        val builder = MediaDescriptionCompat.Builder()
        return usageManager.getMostRatedTracks()
            .flattenAsObservable { it }
            .flatMapMaybe { mediaDao.findTrack(it.trackId) }
            .map {
                val description = it.asMediaDescription(builder, CATEGORY_MOST_RATED)
                MediaItem(description, MediaItem.FLAG_PLAYABLE)
            }
    }
}