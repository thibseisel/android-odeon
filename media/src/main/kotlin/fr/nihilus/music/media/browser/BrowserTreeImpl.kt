/*
 * Copyright 2020 Thibault Seisel
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

package fr.nihilus.music.media.browser

import android.content.Context
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_MOST_RATED
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_POPULAR
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_RECENTLY_ADDED
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ALBUMS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ARTISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_PLAYLISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.core.provider.getResourceUri
import fr.nihilus.music.media.MediaContent
import fr.nihilus.music.media.R
import fr.nihilus.music.media.browser.provider.ChildrenProvider
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Named

@Reusable
internal class BrowserTreeImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("tracks") val tracks: ChildrenProvider,
    @Named("albums") val albums: ChildrenProvider,
    @Named("artists") val artists: ChildrenProvider,
    @Named("playlists") val playlists: ChildrenProvider,
) : BrowserTree {

    /**
     * The tree structure of the media browser.
     */
    private val tree = MediaTree(
        rootId = MediaId.ROOT,
        rootName = context.getString(R.string.browser_root_title)
    ) {
        type(
            TYPE_TRACKS,
            title = context.getString(R.string.tracks_type_title)
        ) {
            val res = context.resources

            category(
                CATEGORY_ALL,
                title = res.getString(R.string.all_music),
                playable = true,
                provider = tracks
            )

            category(
                CATEGORY_MOST_RATED,
                title = res.getString(R.string.most_rated),
                subtitle = res.getString(R.string.most_rated_description),
                iconUri = res.getResourceUri(R.drawable.svc_ic_most_rated_128dp),
                playable = true,
                provider = tracks
            )

            category(
                CATEGORY_POPULAR,
                title = context.getString(R.string.category_popular),
                subtitle = context.getString(R.string.category_popular_description),
                iconUri = null,
                playable = true,
                provider = tracks
            )

            category(
                CATEGORY_RECENTLY_ADDED,
                context.getString(R.string.last_added),
                subtitle = res.getString(R.string.recently_added_description),
                iconUri = res.getResourceUri(R.drawable.svc_ic_most_recent_128dp),
                playable = true,
                provider = tracks
            )
        }

        type(
            TYPE_ALBUMS,
            title = context.getString(R.string.albums_type_title),
            provider = albums
        )

        type(
            TYPE_ARTISTS,
            title = context.getString(R.string.artists_type_title),
            provider = artists
        )

        type(
            TYPE_PLAYLISTS,
            title = context.getString(R.string.playlists_type_title),
            provider = playlists
        )
    }

    override fun getChildren(parentId: MediaId): Flow<List<MediaContent>> =
        tree.getChildren(parentId)

    override suspend fun getItem(itemId: MediaId): MediaContent? = tree.getItem(itemId)
}
