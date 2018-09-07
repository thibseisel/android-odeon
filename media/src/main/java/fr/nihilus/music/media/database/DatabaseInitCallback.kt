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

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.RoomDatabase
import android.support.v4.media.MediaMetadataCompat
import fr.nihilus.music.media.MediaSettings
import fr.nihilus.music.media.source.MusicDao
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

/**
 * Perform database initialization.
 */
class DatabaseInitCallback
@Inject constructor(
    private val prefs: MediaSettings,
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
            Timber.d("First setup: database should be initialized.")
            musicDao.getTracks(null, null)
                .subscribeOn(Schedulers.io())
                .map { metadata ->
                    val mediaId = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                    MusicInfo(mediaId.toLong())
                }.toList()
                .subscribe { stats, error ->
                    if (error == null) {
                        Timber.d("Initializing. Found %d tracks.", stats.size)
                        infoDao.get().addStats(stats)
                        prefs.shouldInitDatabase = stats.isEmpty()
                    } else {
                        Timber.e(error, "An error occurred while initializing database")
                    }
                }
        }
    }

}