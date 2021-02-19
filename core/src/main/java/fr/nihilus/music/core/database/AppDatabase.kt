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

package fr.nihilus.music.core.database

import androidx.room.Database
import androidx.room.Entity
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import fr.nihilus.music.core.database.playlists.Playlist
import fr.nihilus.music.core.database.playlists.PlaylistConverters
import fr.nihilus.music.core.database.playlists.PlaylistDao
import fr.nihilus.music.core.database.playlists.PlaylistTrack
import fr.nihilus.music.core.database.spotify.SpotifyConverters
import fr.nihilus.music.core.database.spotify.SpotifyDao
import fr.nihilus.music.core.database.spotify.SpotifyLink
import fr.nihilus.music.core.database.spotify.TrackFeature
import fr.nihilus.music.core.database.usage.MediaUsageEvent
import fr.nihilus.music.core.database.usage.UsageDao

/**
 * Definition of this application's local database.
 *
 * SQLite tables are generated from classes annotated with [Entity]
 * that are configured in [Database.entities].
 */
@Database(entities = [
    Playlist::class,
    PlaylistTrack::class,
    MediaUsageEvent::class,
    SpotifyLink::class,
    TrackFeature::class
], version = 5)
@TypeConverters(PlaylistConverters::class, SpotifyConverters::class)
abstract class AppDatabase : RoomDatabase() {

    internal abstract val playlistDao: PlaylistDao

    internal abstract val usageDao: UsageDao

    internal abstract val spotifyDao: SpotifyDao

    internal abstract val exclusionDao: TrackExclusionDao

    internal companion object {
        /** The name of the generated SQLite Database. */
        const val NAME = "music.db"
    }
}
