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

package fr.nihilus.music.core.database.spotify

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Define the association between media on the local storage (identified by their [trackId])
 * and media metadata found on a remote server with the given [spotifyId].
 */
@Entity(tableName = "remote_link")
data class SpotifyLink(

    /**
     * The unique identifier of the media stored locally on the device,
     * typically one assigned by the Android MediaStore.
     * If that media is removed from the device, then the corresponding link should be deleted.
     */
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "local_id")
    val trackId: Long,

    /**
     * The unique identifier of the media as given by the remote server.
     * Currently, only Spotify IDs for tracks are supported.
     */
    @ColumnInfo(name = "remote_id")
    val spotifyId: String,

    /**
     * The instant in time at which the association has been created,
     * expressed as the number of seconds elapsed since January the 1st 1970 00:00:00.
     */
    @ColumnInfo(name = "sync_date")
    val syncDate: Long
)