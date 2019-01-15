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

package fr.nihilus.music.base

import android.content.Context
import android.os.Bundle
import android.os.PowerManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import dagger.android.support.DaggerAppCompatActivity
import fr.nihilus.music.settings.UiSettings
import fr.nihilus.music.settings.UiSettings.NightMode
import javax.inject.Inject

/**
 * Base Activity class that supports Dagger injection of class members and into Fragments.
 * This also applies the dark theme if the night mode setting requires it.
 */
abstract class BaseActivity : DaggerAppCompatActivity() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiSettings: UiSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateNightMode()
    }

    override fun onStart() {
        super.onStart()
        updateNightMode()
    }

    private fun updateNightMode() {
        when (val nightModeSetting = uiSettings.nightMode) {
            NightMode.SYSTEM -> delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            NightMode.ALWAYS -> delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> updateNightModeForBatterySaver(nightModeSetting)
        }
    }

    private fun updateNightModeForBatterySaver(nightMode: NightMode) {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isPowerSaveMode) {
            // Since we're in battery saver mode, enable the dark theme
            if (nightMode == NightMode.BATTERY_SAVER_ONLY || nightMode == NightMode.AUTO) {
                delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        } else {
            // If we're not in power saving mode, we can just use the default
            if (nightMode == NightMode.AUTO) {
                delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_AUTO)
            } else {
                delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }
}