package fr.nihilus.mymusic.di;

import android.app.Application;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import fr.nihilus.mymusic.database.AppDatabase;
import fr.nihilus.mymusic.database.MusicInfoDao;
import fr.nihilus.mymusic.database.PlaylistDao;

@Module
public class AppModule {

    @Provides
    @Named("Application")
    Context provideApplicationContext(@NonNull Application app) {
        return app;
    }

    @Provides @Singleton
    SharedPreferences provideSharedPreferences(@NonNull Application app) {
        return PreferenceManager.getDefaultSharedPreferences(app);
    }

    @Provides @Singleton
    AppDatabase provideDatabase(@NonNull Application app) {
        return Room.databaseBuilder(app, AppDatabase.class, AppDatabase.NAME).build();
    }

    @Provides @Singleton
    MusicInfoDao provideMusicInfoDao(@NonNull AppDatabase db) {
        return db.musicInfoDao();
    }

    @Provides @Singleton
    PlaylistDao providePlaylistDao(@NonNull AppDatabase db) {
        return db.playlistDao();
    }
}
