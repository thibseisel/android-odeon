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

package fr.nihilus.music.common.database.playlists

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

/**
 * An association between a music track and a [Playlist].
 * If the containing playlist is deleted so is this playlist track.
 *
 * This object can be associated only to one playlist.
 * To feature the same track in multiple playlists you have to define multiple instances.
 *
 * @constructor
 * Create an association between a music track and a playlist.
 * Note that this class performs no check on the music id and the playlist id.
 * @param playlistId id of the playlist this track belongs to
 * @param trackId id of the music track this object represents
 */
@Entity(tableName = "playlist_track", primaryKeys = ["music_id", "playlist_id"])
@ForeignKey(
    entity = Playlist::class, onDelete = ForeignKey.CASCADE,
    childColumns = ["playlist_id"], parentColumns = ["id"]
)
class PlaylistTrack(

    /**
     * Id of the playlist this track belongs to.
     */
    @ColumnInfo(name = "playlist_id")
    val playlistId: Long,

    /**
     * Id of the music track this object represents.
     */
    @ColumnInfo(name = "music_id")
    val trackId: Long
) {

    /**
     * Position of this track in the playlist.
     */
    @ColumnInfo(name = "position")
    var position: Int = 0

    operator fun component1(): Long = playlistId
    operator fun component2(): Long = trackId
}
