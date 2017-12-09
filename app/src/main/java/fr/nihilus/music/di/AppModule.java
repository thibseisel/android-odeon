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

package fr.nihilus.music.di;

import android.app.Application;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import fr.nihilus.music.database.AppDatabase;
import fr.nihilus.music.database.MusicInfoDao;
import fr.nihilus.music.database.PlaylistDao;
import fr.nihilus.music.media.MediaModule;

/**
 * The main module for this application.
 * It defines dependencies that cannot be instantiated with a constructor,
 * such as implementations for abstract types or calls to factory methods.
 */
@SuppressWarnings("unused")
@Module(includes = {MediaModule.class})
abstract class AppModule {

    @Binds
    abstract Context bindsApplicationContext(@NonNull Application app);

    @Provides @Singleton
    static SharedPreferences provideSharedPreferences(@NonNull Application app) {
        return PreferenceManager.getDefaultSharedPreferences(app);
    }

    @Provides @Singleton
    static AppDatabase provideDatabase(@NonNull Application app) {
        return Room.databaseBuilder(app, AppDatabase.class, AppDatabase.NAME).build();
    }

    @Provides @Singleton
    static MusicInfoDao provideMusicInfoDao(@NonNull AppDatabase db) {
        return db.musicInfoDao();
    }

    @Provides @Singleton
    static PlaylistDao providePlaylistDao(@NonNull AppDatabase db) {
        return db.playlistDao();
    }
}
