/*
 * Copyright 2021 Thibault Seisel
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

package fr.nihilus.music.core.database.exclusion

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A reference to a track that's excluded from the music library.
 * An excluded track is completely ignored by the application and won't be displayed anywhere,
 * even if it was part of an user-defined playlist.
 */
@Entity(tableName = "track_exclusion")
data class TrackExclusion(

    /**
     * Unique identifier of the track that should be excluded from the music library.
     * A track cannot be excluded multiple times.
     */
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "track_id")
    val trackId: Long,

    /**
     * The instant in time at which the track has been excluded,
     * expressed as the number of seconds elapsed since January the 1st 1970 00:00:00.
     */
    @ColumnInfo(name = "exclude_date")
    val excludeDate: Long
)