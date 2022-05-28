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

package fr.nihilus.music.core.database.usage

import androidx.room.ColumnInfo
import androidx.room.Entity
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
class MediaUsageEvent(
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
    @ColumnInfo(name = "track_id")
    val trackId: Long,

    /**
     * The time at which the event occurred, expressed as an Unix Epoch.
     */
    @ColumnInfo(name = "event_time")
    val eventTime: Long
)

/**
 * Summary of the usage events for a given track.
 *
 * @param trackId The unique identifier of the track associated with the recorded events.
 * @param score The total number of events recorded for that track.
 * @param lastEventTime The time at which the last event has been recorded for that track.
 */
data class TrackUsage(

    @ColumnInfo(name = "track_id")
    val trackId: Long,

    @ColumnInfo(name = "event_count")
    val score: Int,

    @ColumnInfo(name = "last_event_time")
    val lastEventTime: Long
)