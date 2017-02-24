package fr.nihilus.mymusic;

import android.app.Application;
import android.os.Build;
import android.support.v7.app.AppCompatDelegate;

import fr.nihilus.mymusic.settings.Prefs;

public class MyApplication extends Application {

    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        AppCompatDelegate.setDefaultNightMode(Prefs.getNightMode(this));
        super.onCreate();

        // Permet d'inflater des VectorDrawable pour API < 21. Peut causer des problèmes.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        }
    }
}
