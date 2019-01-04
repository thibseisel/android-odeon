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

package fr.nihilus.music

import android.support.v7.app.AppCompatDelegate
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import fr.nihilus.music.dagger.DaggerAppComponent
import fr.nihilus.music.settings.UiSettings
import io.reactivex.plugins.RxJavaPlugins
import timber.log.Timber
import javax.inject.Inject

/**
 * An Android Application component that can inject dependencies into Activities and Services.
 * This class also performs general configuration tasks.
 */
class OdeonApplication : DaggerApplication() {
    @Inject lateinit var prefs: UiSettings

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            // Print logs to Logcat
            Timber.plant(Timber.DebugTree())
            // Rethrow unexpected RxJava errors to fail fast
            RxJavaPlugins.setErrorHandler { throw it }
        }

        AppCompatDelegate.setDefaultNightMode(prefs.nightMode)
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> =
        DaggerAppComponent.builder().create(this)
}
