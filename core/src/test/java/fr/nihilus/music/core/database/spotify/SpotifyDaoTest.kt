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

package fr.nihilus.music.core.database.spotify

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.nihilus.music.core.database.AppDatabase
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * Verify the database behavior of [SpotifyDao].
 */
@RunWith(AndroidJUnit4::class)
internal class SpotifyDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: SpotifyDao

    @BeforeTest
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        dao = database.spotifyDao
    }

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    @Test
    @Ignore("Implementation has been fixed in another branch.")
    fun `When deleting links, then also delete associated track features`() = runTest {
        givenInitialLinksAndCorrespondingFeatures()

        dao.deleteLinks(longArrayOf(15L, 10L))

        database.query("SELECT local_id FROM remote_link", null).use { links ->
            links.count shouldBe 1

            links.moveToFirst()
            links.getLong(0) shouldBe 56L
        }

        database.query("SELECT id FROM track_feature", null).use { features ->
            features.count shouldBe 1

            features.moveToFirst()
            features.getString(0) shouldBe "xtDu6k3xNPsb9AyA7PDxb6"
        }
    }

    private suspend fun givenInitialLinksAndCorrespondingFeatures() {
        val initialLinks = listOf(
            SpotifyLink(10L, "nZfFwhP7yRfm0oCXzsyGg6", 0L),
            SpotifyLink(56L, "xtDu6k3xNPsb9AyA7PDxb6", 0L),
            SpotifyLink(15L, "VRaf40qgz9wNAhgKMJHe5e", 0L)
        )
        val initialFeatures = listOf(
            TrackFeature("nZfFwhP7yRfm0oCXzsyGg6", Pitch.D, MusicalMode.MINOR, 174f, 4, -2.72f, 0.02f, 13.1f, 89.4f, 0.0f, 18.4f, 6.36f, 44.4f),
            TrackFeature("xtDu6k3xNPsb9AyA7PDxb6", Pitch.C, MusicalMode.MINOR, 132f, 4, -10.90f, 67.8f, 47.1f, 50.9f, 93.6f, 17.5f, 3.67f, 5.95f),
            TrackFeature("VRaf40qgz9wNAhgKMJHe5e", Pitch.F, MusicalMode.MAJOR, 171f, 4, -6.39f, 0.33f, 49.2f, 95.3f, 0.2f, 13.8f, 21.7f, 63.1f)
        )

        initialLinks.zip(initialFeatures) { link, feature ->
            dao.saveTrackFeature(link, feature)
        }
    }
}
