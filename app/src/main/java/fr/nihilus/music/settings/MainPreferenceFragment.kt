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

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import fr.nihilus.music.BuildConfig
import fr.nihilus.music.R
import fr.nihilus.music.spotify.SpotifySyncWorker
import java.util.*
import kotlin.time.MonoClock

class MainPreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.prefs_main, rootKey)

        if (BuildConfig.DEBUG) {
            findPreference<Preference>("start_sync")!!.setOnPreferenceClickListener {
                val workManager = WorkManager.getInstance(requireContext())

                val request = OneTimeWorkRequestBuilder<SpotifySyncWorker>().build()
                workManager.beginUniqueWork("spotify-sync", ExistingWorkPolicy.KEEP, request).enqueue()
                true
            }
        }
    }
}
