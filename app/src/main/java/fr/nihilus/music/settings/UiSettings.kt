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
import androidx.appcompat.app.AppCompatDelegate
import fr.nihilus.music.R
import javax.inject.Inject
import javax.inject.Singleton

private const val KEY_THEME = "pref_theme"

/**
 * Provides read/write access to UI-related settings.
 */
interface UiSettings {

    /**
     * The current theme preference.
     */
    val appTheme: AppTheme

    /**
     * Initialize theme settings.
     */
    fun setupTheme()

    enum class AppTheme {
        /** Always use the light theme. */
        LIGHT,
        /** Use a dark theme when battery saver is enabled. */
        BATTERY_SAVER_ONLY,
        /** Always use the dark theme. */
        DARK,
        /** Use a light or a dark theme depending on the system configuration. */
        SYSTEM;
    }
}

@Singleton
internal class SharedPreferencesUiSettings
@Inject constructor(
    private val context: Context,
    private val prefs: SharedPreferences
) : UiSettings {

    private val defaultThemeValue = context.getString(R.string.pref_theme_default_value)

    override val appTheme: UiSettings.AppTheme
        get() = getThemeForPrefValue(prefs.getString(KEY_THEME, defaultThemeValue)!!)

    override fun setupTheme() {
        updateUsingThemePreference()
        prefs.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_THEME) {
                updateUsingThemePreference()
            }
        }
    }

    private fun updateUsingThemePreference() {
        AppCompatDelegate.setDefaultNightMode(when (appTheme) {
            UiSettings.AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            UiSettings.AppTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            UiSettings.AppTheme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            UiSettings.AppTheme.BATTERY_SAVER_ONLY -> AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
        })
    }

    private fun getThemeForPrefValue(value: String) = when(value) {
        context.getString(R.string.pref_theme_light_value) -> UiSettings.AppTheme.LIGHT
        context.getString(R.string.pref_theme_dark_value) -> UiSettings.AppTheme.DARK
        context.getString(R.string.pref_theme_battery_value) -> UiSettings.AppTheme.BATTERY_SAVER_ONLY
        context.getString(R.string.pref_theme_system_value) -> UiSettings.AppTheme.SYSTEM
        else -> error("Invalid preference value for theme: $value")
    }
}
