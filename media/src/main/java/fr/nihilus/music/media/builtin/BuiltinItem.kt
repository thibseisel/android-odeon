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

import android.support.v4.media.MediaBrowserCompat
import io.reactivex.Observable
import io.reactivex.Single

/**
 * A static media item defined by the application.
 *
 * Built-in items should be associated with a unique media id that must not change once assigned.
 * Most of the time, built-in items are attached to the root of the media browser
 * and their children have a specific id.
 */
internal interface BuiltinItem {
    /**
     * Returns a representation of this item as a media item used for browsing.
     *
     * The description associated with the media item must be static,
     * for example stored as resources. Note that this media item *might* be playable;
     * but since it represents a built-in element, most of the time it is only browsable.
     *
     * @return a media item representing this built-in item
     */
    fun asMediaItem(): Single<MediaBrowserCompat.MediaItem>

    /**
     * Provides the children media items of this built-in element.
     *
     * Those children media id must be composed of the same root media id
     * plus an extra part specific to each child, except if children are built-in items themselves.
     * In this case the have their own root id.
     *
     * @param parentMediaId The Media ID of the parent item.
     *
     * @return an observable list of media item children of this built-in item
     */
    fun getChildren(parentMediaId: String): Observable<MediaBrowserCompat.MediaItem>
}