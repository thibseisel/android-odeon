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

import android.content.Context
import android.content.SharedPreferences
import fr.nihilus.music.R
import javax.inject.Inject
import javax.inject.Singleton

private const val KEY_STARTUP_SCREEN = "startup_screen"
private const val KEY_SKIP_SILENCE = "skip_silence"

/**
 * Provides read/write access to UI-related settings.
 */
interface UiSettings {
    /**
     * The current enabled/disabled state of the night mode setting.
     */
    val nightMode: NightMode
    /**
     * Defines the screen to be shown to the user when the application starts.
     * Must be the media ID of the media item whose children should be shown on startup.
     */
    val startupScreenMediaId: String
    val shouldSkipSilence: Boolean

    enum class NightMode(val value: String) {
        /** Use a light or a dark theme depending on the system configuration. */
        SYSTEM("system"),
        /** Automatically switch between light and dark theme depending on the current time. */
        AUTO("auto"),
        /** Use a dark theme when battery saver is enabled. */
        BATTERY_SAVER_ONLY("battery"),
        /** Always use the dark theme. */
        ALWAYS("always");
    }
}

@Singleton
internal class SharedPreferencesUiSettings
@Inject constructor(
    private val context: Context,
    private val prefs: SharedPreferences
) : UiSettings {

    override val nightMode: UiSettings.NightMode
        get() = nightModeFromPreferences(prefs.getString(context.getString(R.string.pref_night_mode), null))

    override val startupScreenMediaId: String
        get() = prefs.getString(KEY_STARTUP_SCREEN, context.getString(R.string.pref_default_startup_screen))!!

    override val shouldSkipSilence: Boolean
        get() = prefs.getBoolean(KEY_SKIP_SILENCE, false)

    private fun nightModeFromPreferences(value: String?) =
        UiSettings.NightMode.values().firstOrNull { it.value == value } ?: UiSettings.NightMode.SYSTEM
}
