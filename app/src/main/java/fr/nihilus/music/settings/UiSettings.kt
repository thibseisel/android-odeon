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

package fr.nihilus.music.settings

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.support.v7.app.AppCompatDelegate
import fr.nihilus.music.R
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Singleton

private const val DEFAULT_NIGHT_MODE = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM.toString()
private const val KEY_STARTUP_SCREEN = "startup_screen"
private const val KEY_SKIP_SILENCE = "skip_silence"

/**
 * Provides read/write access to UI-related settings.
 */
interface UiSettings {
    /**
     * The current enabled/disabled state of the night mode setting.
     * This property value is always an `AppCompatDelegate.MODE_NIGHT_*` constant.
     */
    val nightMode: Int
    /**
     * Defines the screen to be shown to the user when the application starts.
     * Must be the media ID of the media item whose children should be shown on startup.
     */
    val startupScreenMediaId: String
    val shouldSkipSilence: Boolean
    val skipSilenceUpdates: Observable<Boolean>
}

@Singleton
internal class SharedPreferencesUiSettings
@Inject constructor(app: Application, private val mPrefs: SharedPreferences) : UiSettings {

    private val appContext: Context = app

    override val nightMode: Int
        get() = mPrefs.getString(appContext.getString(R.string.pref_night_mode), DEFAULT_NIGHT_MODE).toInt()

    override val startupScreenMediaId: String
        get() = mPrefs.getString(KEY_STARTUP_SCREEN, appContext.getString(R.string.pref_default_startup_screen))

    override val shouldSkipSilence: Boolean
        get() = mPrefs.getBoolean(KEY_SKIP_SILENCE, false)

    override val skipSilenceUpdates: Observable<Boolean>
        get() = Observable.create {
            val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key != KEY_SKIP_SILENCE) return@OnSharedPreferenceChangeListener
                it.onNext(shouldSkipSilence)
            }

            mPrefs.registerOnSharedPreferenceChangeListener(preferenceListener)
            it.setCancellable {
                mPrefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)
            }
        }
}
