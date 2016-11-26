package fr.nihilus.mymusic;

import android.app.Application;
import android.support.v7.app.AppCompatDelegate;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);

        // Permet d'inflater des VectorDrawable pour API < 21. Peut causer des problèmes.
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }
}
