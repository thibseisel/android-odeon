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
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.work.*
import fr.nihilus.music.spotify.SpotifySyncWorker

internal class MainPreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.prefs_main, rootKey)

        if (BuildConfig.DEBUG) {
            val startSyncPreference = findPreference<Preference>("start_sync")!!

            val workManager = WorkManager.getInstance(requireContext())
            workManager.getWorkInfosForUniqueWorkLiveData("spotify-sync").observe(this) { allWorkInfo ->
                allWorkInfo.firstOrNull()?.state?.let { workState ->
                    startSyncPreference.isEnabled = workState.isFinished

                    startSyncPreference.summary = when (workState) {
                        WorkInfo.State.ENQUEUED -> getString(R.string.dev_sync_summary_waiting)
                        WorkInfo.State.RUNNING -> getString(R.string.dev_sync_summary_running)
                        WorkInfo.State.SUCCEEDED -> getString(R.string.dev_sync_summary_success)
                        WorkInfo.State.FAILED -> getString(R.string.dev_sync_summary_failure)
                        else -> null
                    }
                }
            }

            startSyncPreference.setOnPreferenceClickListener {
                scheduleSpotifySync(workManager)
                true
            }
        }
    }

    private fun scheduleSpotifySync(workManager: WorkManager) {
        val connectedNetworkConstraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SpotifySyncWorker>()
            .setConstraints(connectedNetworkConstraint)
            .build()

        workManager.beginUniqueWork("spotify-sync", ExistingWorkPolicy.KEEP, request).enqueue()
    }
}
