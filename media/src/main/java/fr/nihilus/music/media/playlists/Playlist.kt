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

package fr.nihilus.music.media.playlists

import android.net.Uri
import android.support.v4.media.MediaDescriptionCompat
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import fr.nihilus.music.media.CATEGORY_PLAYLISTS
import fr.nihilus.music.media.mediaIdOf

/**
 * A class that groups information associated with a playlist.
 * Each playlist has an unique identifier that its [PlaylistTrack] children must reference
 * to be included.
 */
@Entity(tableName = "playlist")
internal data class Playlist(

    /**
     * The unique identifier of this playlist.
     * If this playlist has not been saved, this identifier will be `null`.
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long?,

    /**
     * The title given by the user to this playlist.
     */
    @ColumnInfo(name = "title")
    val title: String,

    /**
     * The date at which this playlist has been created.
     */
    @ColumnInfo(name = "date_created")
    val created: Long,

    /**
     * Uri pointing to a Bitmap that illustrates this playlist.
     */
    @ColumnInfo(name = "icon_uri")
    val iconUri: Uri?
) {

    /**
     * Create a new unsaved playlist that has the given [title] and an optional icon.
     *
     * @param title The title of the new playlist.
     * @param iconUri The Uri pointing to an image file that represents the playlist.
     */
    @Ignore
    constructor(
        title: CharSequence,
        iconUri: Uri? = null
    ) : this(null, title.toString(), System.currentTimeMillis(), iconUri)

    fun asMediaDescription(builder: MediaDescriptionCompat.Builder): MediaDescriptionCompat {
        val playlistId = checkNotNull(id) { "Cant create MediaDescription of an unsaved playlist" }
        val mediaId = mediaIdOf(CATEGORY_PLAYLISTS, playlistId.toString())
        return builder.setMediaId(mediaId)
            .setTitle(title)
            .setIconUri(iconUri)
            .build()
    }
}
