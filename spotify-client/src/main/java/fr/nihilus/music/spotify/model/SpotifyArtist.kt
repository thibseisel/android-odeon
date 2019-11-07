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
import com.squareup.moshi.JsonClass

/**
 * Metadata of an artist from the Spotify API.
 */
@JsonClass(generateAdapter = true)
internal class SpotifyArtist(

    /**
     * The unique identifier of this artist on Spotify servers.
     */
    @Json(name = "id")
    val id: String,

    /**
     * The name of this artist.
     */
    @Json(name = "name")
    val name: String,

    /**
     * The popularity of the artist.
     * The value will be between `0` and `100`, with `100` being the most popular.
     * The artist’s popularity is calculated from the popularity of all the artist’s tracks.
     */
    @Json(name = "popularity")
    val popularity: Int,

    /**
     * A list of the genres the artist is associated with.
     * For example: "Prog Rock" , "Post-Grunge".
     * (If not yet classified, the array is empty.)
     */
    @Json(name = "genres")
    val genres: List<String>,

    /**
     * Images of the artist in various sizes, widest first.
     */
    @Json(name = "images")
    val images: List<Image>
) {
    override fun toString(): String = "SpotifyArtist[id=$id, name=$name]"
}

