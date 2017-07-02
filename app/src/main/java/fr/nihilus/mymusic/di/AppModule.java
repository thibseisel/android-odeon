package fr.nihilus.mymusic.di;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {

    @Provides @Singleton
    SharedPreferences provideSharedPreferences(@NonNull Application app) {
        return PreferenceManager.getDefaultSharedPreferences(app);
    }
}
