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

package fr.nihilus.music.common.test.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import fr.nihilus.music.common.database.AppDatabase
import fr.nihilus.music.common.database.DatabaseModule
import javax.inject.Singleton

/**
 * Provides a shared connection to an in-memory SQLite database.
 * This database instance allows performing queries on the Main Thread.
 */
@Module(includes = [DatabaseModule::class])
internal object InMemoryDatabaseModule {

    @Provides @Singleton
    fun providesInMemoryDatabase(context: Context): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}