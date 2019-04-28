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

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB = "music.test.db"

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    @[JvmField Rule]
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

        //language=RoomSql
        db.execSQL("""
            INSERT INTO playlist (id, title, date_created, date_last_played, art_uri)
            VALUES (42, 'Summertime', 1556447033, NULL, 'path/to/icon.png')
        """.trimIndent())

        db.close()
        db = helper.runMigrationsAndValidate(TEST_DB, 3, false, AppDatabase.MIGRATION_2_3)

        // Check that the playlist was properly migrated.
        val cursor = db.query("SELECT id, title, date_created, icon_uri FROM playlist")
        assertThat(cursor.moveToFirst()).isTrue()
        assertThat(cursor.count).isEqualTo(1)

        assertThat(cursor.getLong(0)).isEqualTo(42L)
        assertThat(cursor.getString(1)).isEqualTo("Summertime")
        assertThat(cursor.getLong(2)).isEqualTo(1556447033L)
        assertThat(cursor.getString(3)).isEqualTo("path/to/icon.png")
    }
}