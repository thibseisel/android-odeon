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

package fr.nihilus.music.spotify

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.*
import fr.nihilus.music.core.worker.SingleWorkerFactory
import fr.nihilus.music.spotify.manager.SpotifyManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

/**
 * A tasks for deferring download of media metadata from the Spotify API.
 * The execution of this task requires an internet connection.
 */
class SpotifySyncWorker(
    context: Context,
    params: WorkerParameters,
    private val manager: SpotifyManager
) : CoroutineWorker(context, params) {

    private companion object {
        const val CHANNEL_ID = "fr.nihilus.music.spotify.SPOTIFY_SYNC"
        const val NOTIFICATION_ID = 42
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val cancelSyncAction = NotificationCompat.Action(
        R.drawable.sfy_ic_cancel_24,
        context.getString(R.string.sfy_action_cancel_sync),
        WorkManager.getInstance(context).createCancelPendingIntent(id)
    )

    override suspend fun doWork(): Result {
        promoteToForeground()

        return try {
            manager.sync()
                .conflate()
                .collect { progress ->
                    val intermediateProgressNotification = createProgressNotification(
                        progress = progress.success + progress.failures,
                        max = progress.total
                    )

                    notificationManager.notify(NOTIFICATION_ID, intermediateProgressNotification)

                    // Prevent from updating notification more than once per second.
                    // This should be replaced by a suitable "throttleLatest" flow operator.
                    delay(500)
                }

            Result.success()

        } catch (syncFailure: Exception) {
            Timber.e(syncFailure, "An error occurred while syncing with Spotify.")
            Result.failure()
        }
    }

    @SuppressLint("InlinedApi")
    private suspend fun promoteToForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initNotificationChannel()
        }

        setForeground(
            ForegroundInfo(
                NOTIFICATION_ID,
                createProgressNotification(progress = 0, max = 0),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        )
    }

    private fun createProgressNotification(progress: Int, max: Int): Notification {
        if (BuildConfig.DEBUG) {
            check(max >= 0)
            check(progress in 0..max)
        }

        val context = applicationContext
        val notificationTitle = context.getString(R.string.sfy_sync_notification_title)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentTitle(notificationTitle)
            .setTicker(notificationTitle)
            .setSmallIcon(R.drawable.sfy_ic_sync_24)
            .setProgress(max, progress, max == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(cancelSyncAction)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initNotificationChannel() {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.sfy_channel_analysis),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    internal class Factory @Inject constructor(
        private val manager: Provider<SpotifyManager>
    ) : SingleWorkerFactory {

        override fun createWorker(appContext: Context, params: WorkerParameters): ListenableWorker {
            return SpotifySyncWorker(appContext, params, manager.get())
        }
    }
}