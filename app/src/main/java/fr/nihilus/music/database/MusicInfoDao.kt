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

package fr.nihilus.music.database

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.support.annotation.WorkerThread

/**
 * Defines database interactions for storing statistics on tracks from the music library.
 */
@Dao
interface MusicInfoDao {

    /**
     * Add new statistic records to the database.
     * If a record already exists, it will not be replaced.
     *
     * @param stats Statistics of tracks from the music library.
     */
    @WorkerThread
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addStats(stats: Iterable<MusicInfo>)
}
