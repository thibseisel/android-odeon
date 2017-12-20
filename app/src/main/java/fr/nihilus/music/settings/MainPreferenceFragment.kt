/*
 * Copyright 2017 Thibault Seisel
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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.preference.PreferenceFragmentCompat
import dagger.android.support.AndroidSupportInjection
import fr.nihilus.music.R
import javax.inject.Inject

class MainPreferenceFragment : PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject lateinit var prefs: PreferenceDao
    private lateinit var keyNightMode: String

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs_main)
        keyNightMode = context!!.getString(R.string.pref_night_mode)
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        if (keyNightMode == key) {
            onNightModeChanged(this.prefs.nightMode)
        }
    }

    private fun onNightModeChanged(@AppCompatDelegate.NightMode newMode: Int) {
        AppCompatDelegate.setDefaultNightMode(newMode)
        val hostActivity = (activity as AppCompatActivity)
        hostActivity.delegate.applyDayNight()

        val intent = Intent()
        intent.putExtra("night_mode", newMode)

        hostActivity.setResult(Activity.RESULT_OK, intent)
    }
}
