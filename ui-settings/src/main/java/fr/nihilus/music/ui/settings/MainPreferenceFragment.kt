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

package fr.nihilus.music.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import fr.nihilus.music.core.settings.Settings
import fr.nihilus.music.ui.settings.exclusion.ExcludedTracksFragment
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
internal class MainPreferenceFragment : PreferenceFragmentCompat() {
    @Inject internal lateinit var settings: Settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply theme whenever it is changed via preferences.
        settings.currentTheme
            .drop(1)
            .onEach(::applyTheme)
            .launchIn(lifecycleScope)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.prefs_main, rootKey)

        requirePreference<Preference>(PREF_KEY_TRACK_EXCLUSION).setOnPreferenceClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.content_fragment, ExcludedTracksFragment())
                .addToBackStack(null)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit()
            true
        }
    }

    private fun applyTheme(theme: Settings.AppTheme) {
        AppCompatDelegate.setDefaultNightMode(
            when (theme) {
                Settings.AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                Settings.AppTheme.BATTERY_SAVER_ONLY -> AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                Settings.AppTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    private fun <P : Preference> requirePreference(key: String): P = findPreference(key)
        ?: error("Attempt to find a non-existing preference named \"$key\"")
}

private const val PREF_KEY_TRACK_EXCLUSION = "track_exclusion"
