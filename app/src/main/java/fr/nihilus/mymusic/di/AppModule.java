package fr.nihilus.mymusic.di;

import android.app.Application;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import fr.nihilus.mymusic.database.AppDatabase;
import fr.nihilus.mymusic.database.MusicInfoDao;
import fr.nihilus.mymusic.database.PlaylistDao;
import fr.nihilus.mymusic.media.LruMusicCache;
import fr.nihilus.mymusic.media.MusicCache;

@Module
abstract class AppModule {

    @Provides
    @Named("Application")
    static Context provideApplicationContext(@NonNull Application app) {
        return app;
    }

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

    @Binds @Singleton
    abstract MusicCache bindsMusicCache(LruMusicCache lruCache);
}
