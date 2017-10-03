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
