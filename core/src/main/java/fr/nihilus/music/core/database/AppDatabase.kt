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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    internal companion object {
        /** The name of the generated SQLite Database. */
        const val NAME = "music.db"

        /**
         * Define instructions to be executed on the database to migrate from schema v1 to v2:
         * - Delete the `music_info` table.
         * - Create the `usage_event` table.
         * - Remove index on `playlist.title` column.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) = with(database) {
                // Delete the obsolete and unused table "music_info".
                execSQL("DROP TABLE IF EXISTS music_info")
                // Create the new table "usage_event" to match the "UsageEvent" entity.
                execSQL("CREATE TABLE IF NOT EXISTS `usage_event` (`event_uid` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `track_id` TEXT NOT NULL, `event_time` INTEGER NOT NULL)")
                execSQL("CREATE INDEX IF NOT EXISTS `index_usage_event_track_id` ON `usage_event` (`track_id`)")

                // Remove unique constraint index on playlist titles
                execSQL("DROP INDEX `index_playlist_title`")
            }
        }

        /**
         * Recreate the playlist table to change its schema:
         * - remove unused column `date_latest_played`
         * - rename column `art_uri` to `icon_uri`.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) = with(database) {
                // Recreate the whole playlist table without the unused "date_last_played" column.
                // All data are copied to the "playlist_tmp" table with the new schema, then the old table is dropped.
                // This is necessary because SQLite doesn't support deleting columns.
                execSQL("CREATE TABLE `playlist_tmp` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `title` TEXT NOT NULL, `date_created` INTEGER NOT NULL, `icon_uri` TEXT)")
                execSQL("INSERT INTO `playlist_tmp` SELECT id, title, date_created, art_uri AS icon_uri FROM playlist")
                execSQL("DROP TABLE `playlist`")
                execSQL("ALTER TABLE `playlist_tmp` RENAME TO `playlist`")

                // Recreate the whole usage_event table with its track_id column type changed to INTEGER.
                execSQL("CREATE TABLE IF NOT EXISTS `usage_event_tmp` (`event_uid` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `track_id` INTEGER NOT NULL, `event_time` INTEGER NOT NULL)")
                execSQL("INSERT INTO `usage_event_tmp` SELECT event_uid, CAST(track_id AS INTEGER), event_time FROM usage_event")
                execSQL("DROP INDEX `index_usage_event_track_id`")
                execSQL("DROP TABLE `usage_event`")
                execSQL("ALTER TABLE `usage_event_tmp` RENAME TO `usage_event`")
            }
        }

        /**
         * Define instructions to be executed on the database to migrate from schema v3 to v4:
         * - create the new table `remote_link`
         * - create the new table `track_feature`
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) = with(database) {
                // Create new tables "remote_link" and "track_feature".
                execSQL("CREATE TABLE IF NOT EXISTS `remote_link` (`local_id` INTEGER NOT NULL, `remote_id` TEXT NOT NULL, `sync_date` INTEGER NOT NULL, PRIMARY KEY(`local_id`))")
                execSQL("CREATE TABLE IF NOT EXISTS `track_feature` (`id` TEXT NOT NULL, `key` INTEGER, `mode` INTEGER NOT NULL, `tempo` REAL NOT NULL, `time_signature` INTEGER NOT NULL, `loudness` REAL NOT NULL, `acousticness` REAL NOT NULL, `danceability` REAL NOT NULL, `energy` REAL NOT NULL, `instrumentalness` REAL NOT NULL, `liveness` REAL NOT NULL, `speechiness` REAL NOT NULL, `valence` REAL NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        /**
         * Schema migrations related to playlists.
         * 1. Recreate "playlist" table to enforce non-null primary key.
         * 2. Recreate "playlist_track" table to enforce a foreign key constraint on "playlist_id".
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) = with(database) {
                // Recreate the "playlist" table.
                execSQL("ALTER TABLE `playlist` RENAME TO `playlist_old`")
                execSQL("CREATE TABLE `playlist` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `date_created` INTEGER NOT NULL, `icon_uri` TEXT)")
                execSQL("INSERT INTO `playlist` SELECT * FROM `playlist_old` WHERE id IS NOT NULL")
                execSQL("DROP TABLE `playlist_old`")

                // Recreate the "playlist_track" table, copying only tracks whose "playlist_id"
                // matches an "id" in the "playlist" table.
                // Also add index to playlist_id to avoid full table scan when deleting playlists.
                execSQL("ALTER TABLE `playlist_track` RENAME TO `playlist_track_old`")
                execSQL("CREATE TABLE `playlist_track` (`position` INTEGER NOT NULL, `playlist_id` INTEGER NOT NULL, `music_id` INTEGER NOT NULL, PRIMARY KEY(`music_id`, `playlist_id`), FOREIGN KEY(`playlist_id`) REFERENCES `playlist`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                execSQL("CREATE INDEX `index_playlist_track_playlist_id` ON `playlist_track` (`playlist_id`)")
                execSQL("INSERT INTO `playlist_track` SELECT * FROM `playlist_track_old` WHERE playlist_id IN (SELECT id FROM `playlist`)")
                execSQL("DROP TABLE `playlist_track_old`")
            }
        }
    }
}
