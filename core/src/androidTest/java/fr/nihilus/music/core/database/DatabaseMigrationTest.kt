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

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val TEST_DB = "music.test.db"

@MediumTest
class DatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migration1To2() {
        val db = helper.createDatabase(TEST_DB, 1)
        // Playlists are not impacted by this migration.
        // No test data are needed.

        // Close the database to prepare for the migration.
        db.close()

        // Re-open the database with version 2, execute and validate the migration from v1 to v2.
        helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2)
    }

    @Test
    fun migration2To3() {
        var db = helper.createDatabase(TEST_DB, 2)

        db.execSQL("""
            INSERT INTO playlist (id, title, date_created, date_last_played, art_uri) 
            VALUES (42, 'Summertime', 1556447033, NULL, 'path/to/icon.png')
        """)
        db.execSQL("""
            INSERT INTO usage_event (event_uid, track_id, event_time) 
            VALUES (12, '123', 1556447033)
        """)

        db.close()
        db = helper.runMigrationsAndValidate(TEST_DB, 3, false, AppDatabase.MIGRATION_2_3)

        // Check that the playlist has been properly migrated.
        db.query("SELECT id, title, date_created, icon_uri FROM playlist").use {
            assertTrue(it.moveToFirst())
            assertEquals(42L, it.getLong(0))
            assertEquals("Summertime", it.getString(1))
            assertEquals(1556447033L, it.getLong(2))
            assertEquals("path/to/icon.png", it.getString(3))
        }

        // Check that usage events have been properly migrated.
        db.query("SELECT event_uid, track_id, event_time FROM usage_event").use {
            assertTrue(it.moveToFirst())
            assertEquals(12L, it.getLong(0))
            assertEquals(123L, it.getLong(1))
            assertEquals(1556447033L, it.getLong(2))
        }
    }

    @Test
    fun migration3To4() {
        val db = helper.createDatabase(TEST_DB, 3)
        db.close()

        // Check that "remote_link" and "track_feature" tables have been added.
        helper.runMigrationsAndValidate(TEST_DB, 3, false, AppDatabase.MIGRATION_3_4)
    }

    @Test
    fun migration4To5() {
        var db = helper.createDatabase(TEST_DB, 4)

        db.execSQL("""
            INSERT INTO playlist (id, title, date_created, icon_uri) 
            VALUES (42, 'Zen', 1585742400, 'content://fr.nihilus.music.provider/icons/zen.png')
        """)

        db.execSQL("""
            INSERT INTO playlist_track (playlist_id, music_id, position)
            VALUES (42, 16, 1), (31, 16, 1), (42, 19, 2), (89, 19, 7)
        """)

        db.close()
        db = helper.runMigrationsAndValidate(TEST_DB, 5, false, AppDatabase.MIGRATION_4_5)

        // Check that the playlist has been properly migrated.
        db.query("SELECT id, title, date_created, icon_uri FROM playlist").use {
            assertTrue(it.moveToFirst())

            assertEquals(42L, it.getLong(0))
            assertEquals("Zen", it.getString(1))
            assertEquals(1585742400L, it.getLong(2))
            assertEquals("content://fr.nihilus.music.provider/icons/zen.png", it.getString(3))
        }

        // Check that playlist tracks have been migrated, ignoring orphan records.
        db.query("SELECT playlist_id, music_id, position FROM playlist_track").use {
            assertEquals(2, it.count)

            it.moveToPosition(0)
            assertEquals(42L, it.getLong(0))
            assertEquals(16L, it.getLong(1))
            assertEquals(1, it.getInt(2))

            it.moveToPosition(1)
            assertEquals(42L, it.getLong(0))
            assertEquals(19, it.getLong(1))
            assertEquals(2, it.getInt(2))
        }
    }
}