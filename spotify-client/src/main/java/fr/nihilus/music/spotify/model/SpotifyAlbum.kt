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
 * Metadata of an album from the Spotify API.
 */
@JsonClass(generateAdapter = true)
internal class SpotifyAlbum(

    /**
     * The unique identifier of this album on Spotify servers.
     */
    @Json(name = "id")
    val id: String,

    /**
     * The name of the album.
     */
    @Json(name = "name")
    val name: String,

    /**
     * The date the album was first released, for example `1981`.
     * Depending on the precision, it might be shown as `1981-12` or `1981-12-15`.
     */
    @Json(name = "release_date")
    val releaseDate: String,

    /**
     * The precision with which release_date value is known: year, month, or day.
     */
    @Json(name = "release_date_precision")
    val releaseDatePrecision: String,

    /**
     * The cover art for the album in various sizes, widest first.
     */
    @Json(name = "images")
    val images: List<Image>
) {
    override fun toString(): String = "SpotifyAlbum[$id=id, name$name]"
}