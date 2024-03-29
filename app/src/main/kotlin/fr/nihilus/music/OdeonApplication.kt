/*
 * Copyright 2020 Thibault Seisel
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

import android.app.Application
import android.os.StrictMode
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * An Android Application component that can inject dependencies into Activities and Services.
 * This class also performs general configuration tasks.
 */
@HiltAndroidApp
class OdeonApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)

        if (BuildConfig.DEBUG) {
            // Print logs to Logcat
            Timber.plant(Timber.DebugTree())
            StrictMode.enableDefaults()
        }
    }
}
