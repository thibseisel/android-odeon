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

import android.arch.persistence.room.Room
import dagger.Module
import dagger.Provides
import fr.nihilus.music.media.di.ServiceScoped
import fr.nihilus.music.media.service.MusicService

@Module
internal class DatabaseModule {

    @[Provides ServiceScoped]
    fun provideDatabase(service: MusicService): AppDatabase =
        Room.databaseBuilder(service, AppDatabase::class.java, AppDatabase.NAME).build()

    @[Provides ServiceScoped]
    fun providePlaylistDao(db: AppDatabase): PlaylistDao = db.playlistDao
}
