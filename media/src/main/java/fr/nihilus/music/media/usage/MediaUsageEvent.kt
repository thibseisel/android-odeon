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

package fr.nihilus.music.media.usage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/**
 * Represents the recording of a media usage event.
 * Such events provide information on the user's preferred tracks.
 *
 * @constructor Generate an usage event for a given track at the given time.
 * This constructor is intended to be called by Room DAOs.
 * To create new events that are not persisted to the database, use the convenience constructor.
 *
 * @param uid The unique identifier assigned to this event record by the database.
 * This should be `0` if an equivalent event has not been persisted to the database.
 * @param trackId The unique identifier of the track that generated this event.
 * This should be a valid track from the music library.
 * @param eventTime The time at which the event occurred, expressed as
 * the number of seconds since `1970-01-01 00:00:00 UTC`.
 */
@Entity(tableName = "usage_event")
internal class MediaUsageEvent(
    /**
     * The unique identifier for this event record in the database.
     * When not persisted, this is `0`.
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "event_uid")
    val uid: Long,

    /**
     * The unique identifier of the track that generated the event.
     */
    @ColumnInfo(name = "track_id", index = true)
    val trackId: String,

    /**
     * The time at which the event occurred, expressed as an Unix Epoch.
     */
    @ColumnInfo(name = "event_time")
    val eventTime: Long
) {
    /**
     * Convenience constructor to instantiate a new usage event that occurred just now.
     * The event [uid] will be initialized to `0` (unsaved) and the [eventTime] to the current time.
     *
     * @param trackId The unique identifier of the track that generated this event.
     */
    @Ignore
    constructor(trackId: String) : this(0L, trackId, System.currentTimeMillis() / 1000L)
}

/**
 * A track associated with a given numeric score.
 * The higher the score, the more the related track is appreciated by users.
 */
internal class TrackScore(
    @ColumnInfo(name = "track_id")
    val trackId: Long,

    @ColumnInfo(name = "event_count")
    val score: Int
)