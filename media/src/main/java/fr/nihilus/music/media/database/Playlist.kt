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

package fr.nihilus.music.media.database

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import android.net.Uri
import android.support.v4.media.MediaDescriptionCompat
import fr.nihilus.music.media.CATEGORY_PLAYLISTS
import fr.nihilus.music.media.mediaIdOf
import java.util.*

/**
 * A class that groups information associated with a playlist.
 * Each playlist has an unique identifier that its [PlaylistTrack] children must reference
 * to be included.
 */
@Entity(
    tableName = "playlist", indices = [
        Index(value = ["title"], unique = true)
    ]
)
class Playlist {

    /**
     * The unique identifier of this playlist.
     * If this playlist has not been saved, this identifier will be `null`.
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long? = null

    /**
     * The title given by the user to this playlist.
     */
    @ColumnInfo(name = "title")
    var title: String = ""

    /**
     * The date at which this playlist has been created.
     */
    @ColumnInfo(name = "date_created")
    var created: Date = Date()

    /**
     * The date a which this playlist has been played for the last time.
     * This will be `null` if this playlist has never been played.
     */
    @ColumnInfo(name = "date_last_played")
    var lastPlayed: Date? = null

    /**
     * URI pointing to a Bitmap that features this playlist.
     */
    @ColumnInfo(name = "art_uri")
    var artUri: Uri = Uri.EMPTY

    fun asMediaDescription(builder: MediaDescriptionCompat.Builder): MediaDescriptionCompat {
        val playlistId = checkNotNull(id) { "Cant create MediaDescription of an unsaved playlist" }
        val mediaId = mediaIdOf(CATEGORY_PLAYLISTS, playlistId.toString())
        return builder.setMediaId(mediaId)
            .setTitle(title)
            .setIconUri(artUri)
            .build()
    }

    companion object {
        /**
         * Create a new playlist from the given title without saving it.
         * @param playlistTitle Title to given to this playlist.
         */
        fun create(playlistTitle: CharSequence): Playlist {
            return Playlist().apply {
                title = playlistTitle.toString()
                created = Date()
            }
        }
    }
}
