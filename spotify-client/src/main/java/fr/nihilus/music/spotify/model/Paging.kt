/*
 * Copyright 2019 Thibault Seisel
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

package fr.nihilus.music.spotify.model

import com.squareup.moshi.Json

internal class Paging<T>(

    /**
     * The requested data.
     */
    @Json(name = "items")
    val items: List<T>,

    /**
     * The offset of the items returned (as set in the query or by default).
     */
    @Json(name = "offset")
    val offset: Int,

    /**
     * The URL to request the next page of results.
     * This will be `null` if this page is the last one.
     */
    @Json(name = "next")
    val next: String?,

    /**
     * The maximum number of items in the response (as set in the query or by default).
     */
    @Json(name = "limit")
    val limit: Int,

    /**
     * The maximum number of items available to return.
     */
    @Json(name = "total")
    val total: Int
)

internal class SearchResults(
    @Json(name = "artists")
    val artists: Paging<Artist>,

    @Json(name = "albums")
    val albums: Paging<Album>,

    @Json(name = "tracks")
    val tracks: Paging<Track>
)