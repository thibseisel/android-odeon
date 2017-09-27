package fr.nihilus.music

import android.app.Activity
import android.app.Application
import android.app.Service
import android.os.Build
import android.support.v7.app.AppCompatDelegate
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasServiceInjector
import fr.nihilus.music.di.DaggerAppComponent
import fr.nihilus.music.settings.PreferenceDao
import javax.inject.Inject

/**
 * An Android Application component that can inject dependencies into Activities and Services.
 * This class also performs general configuration tasks.
 */
class NihilusMusicApplication : Application(), HasActivityInjector, HasServiceInjector {

    @Inject lateinit var dispatchingActivityInjector: DispatchingAndroidInjector<Activity>
    @Inject lateinit var dispatchingServiceInjector: DispatchingAndroidInjector<Service>
    @Inject lateinit var mPrefs: PreferenceDao

    override fun onCreate() {
        super.onCreate()

        DaggerAppComponent.builder()
                .application(this)
                .build()
                .inject(this)

        AppCompatDelegate.setDefaultNightMode(mPrefs.nightMode)

        // Permet d'inflater des VectorDrawable pour API < 21. Peut causer des problèmes.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }

    override fun activityInjector() = dispatchingActivityInjector

    override fun serviceInjector() = dispatchingServiceInjector
}
