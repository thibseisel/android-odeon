/*
 * Copyright 2017 Thibault Seisel
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

package fr.nihilus.music.database

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.RoomDatabase
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import fr.nihilus.music.media.source.MusicDao
import fr.nihilus.music.settings.PreferenceDao
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Provider

private const val TAG = "DbInit"

/**
 * Perform database initialization.
 */
class DatabaseInitCallback
@Inject constructor(
        private val prefs: PreferenceDao,
        private val musicDao: MusicDao,
        private val infoDao: Provider<MusicInfoDao>
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        initStats()
    }

    /**
     * Create statistics for each track currently in the music library.
     * If database has already been initialized, nothing will happen.
     */
    private fun initStats() {
        if (prefs.shouldInitDatabase) {
            Log.d(TAG, "Database should be initialized.")
            musicDao.getTracks(null, null)
                    .subscribeOn(Schedulers.io())
                    .map { metadata ->
                        val mediaId = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                        MusicInfo(mediaId.toLong())
                    }.toList()
                    .subscribe { stats, error ->
                        if (error == null) {
                            Log.d(TAG, "Initializing. Found ${stats.size} tracks.")
                            infoDao.get().addStats(stats)
                            prefs.shouldInitDatabase = stats.isEmpty()
                        } else {
                            Log.e(TAG, "An error occurred while initializing database", error)
                        }
                    }
        }
    }

}