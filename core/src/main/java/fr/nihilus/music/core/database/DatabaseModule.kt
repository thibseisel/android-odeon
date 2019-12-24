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

package fr.nihilus.music.core.database

import dagger.Module
import dagger.Provides
import fr.nihilus.music.core.database.playlists.PlaylistDao
import fr.nihilus.music.core.database.spotify.SpotifyDao
import fr.nihilus.music.core.database.usage.UsageDao

/**
 * Provides implementations of [Dao][androidx.room.Dao]-annotated classes.
 * This module has a dependency on [AppDatabase] and should be included in a module that provides it.
 */
@Module
object DatabaseModule {

    @Provides
    internal fun providePlaylistDao(db: AppDatabase): PlaylistDao = db.playlistDao

    @Provides
    internal fun provideMediaUsageDao(db: AppDatabase): UsageDao = db.usageDao

    @Provides
    internal fun provideSpotifyDao(db: AppDatabase): SpotifyDao = db.spotifyDao
}
