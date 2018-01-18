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

package fr.nihilus.music.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.app.NotificationCompat.MediaStyle
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import fr.nihilus.music.HomeActivity
import fr.nihilus.music.R
import fr.nihilus.music.di.ServiceScoped
import fr.nihilus.music.utils.loadResourceAsBitmap
import javax.inject.Inject

@ServiceScoped
class MediaNotificationManager
@Inject constructor(private val service: MusicService) {

    companion object {
        private const val TAG = "MediaNotifMgr"
        const val REQUEST_CODE = 100
        private const val NOTIFICATION_ID = 42
        private const val CHANNEL_ID = "media_channel"
        private const val UPDATE_DELAY = 100L
        private const val ICON_SIZE = 320
    }

    private val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

    private val pauseAction = NotificationCompat.Action(
        R.drawable.ic_pause_48dp,
        service.getString(R.string.action_pause),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_PAUSE
        )
    )

    private val playAction = NotificationCompat.Action(
        R.drawable.ic_play_arrow_48dp,
        service.getString(R.string.action_play),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_PLAY
        )
    )

    private val previousAction = NotificationCompat.Action(
        R.drawable.ic_skip_previous_36dp,
        service.getString(R.string.action_previous),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        )
    )

    private val nextAction = NotificationCompat.Action(
        R.drawable.ic_skip_next_36dp,
        service.getString(R.string.action_next),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        )
    )

    private val emptyAction = NotificationCompat.Action(0, null, null)

    private val defaultLargeIcon = loadResourceAsBitmap(
        service, R.drawable.ic_default_icon,
        ICON_SIZE, ICON_SIZE
    )

    private val contentIntent = PendingIntent.getActivity(
        service, REQUEST_CODE,
        Intent(service, HomeActivity::class.java),
        PendingIntent.FLAG_CANCEL_CURRENT
    )

    private val controllerCallback = ControllerCallback()
    private val publishHandler = Handler()

    private var isForeground = false

    private val updateNotificationTask = Runnable {
        val controller = service.session.controller
        val metadata = controller.metadata
        val state = controller.playbackState

        publishNotification(state, metadata) { notification ->
            notificationManager.notify(NOTIFICATION_ID, notification)
        }

    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }
        // Cancel all notifications to handle the case where the Service was killed
        // and restarted by the system.
        notificationManager.cancelAll()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {

            val name = service.getString(R.string.channel_mediasession)
            val channelDescription = service.getString(R.string.channel_mediasession_description)
            val importance = NotificationManager.IMPORTANCE_LOW

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = channelDescription
                enableLights(true)
                lightColor = Color.GREEN
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Start the display of a persistent notification with playback control abilities.
     * This notification will immediately promote the music service to the foreground while playing,
     * making it impossible to be destroyed when device is low on memory.
     *
     * While service stays on the foreground, notification will update automatically
     * based on state and metadata updates forwarded by the media session.
     */
    fun start() {
        if (!isForeground) {
            // Immediately publish the notification so that the service is promoted to the foreground
            val controller = service.session.controller
            val metadata = controller.metadata
            val playbackState = controller.playbackState

            publishNotification(playbackState, metadata) { notification ->
                // Make service foreground and listen to changes in metadata and playback state.
                controller.registerCallback(controllerCallback)
                service.startForeground(NOTIFICATION_ID, notification)
                isForeground = true
            }

        } else {
            // Schedule a notification update
            scheduleNotificationUpdate()
        }
    }

    /**
     * Put the music service to the background, removing its foreground state.
     *
     * @param clearNotification Whether the notification should also be removed
     * from the notification panel.
     */
    fun stop(clearNotification: Boolean) {
        if (isForeground) {
            Log.d(TAG, "clearNotification: FOREGROUND => false")
            isForeground = false
            service.stopForeground(clearNotification)
        }

        if (clearNotification) {
            publishHandler.removeCallbacks(updateNotificationTask)
            service.session.controller.unregisterCallback(controllerCallback)
        }
    }

    /**
     * Schedule a notification update, canceling any pending request.
     * A delay is applied so that notifications are not updated too often (which causes UI lags).
     */
    private fun scheduleNotificationUpdate() {
        publishHandler.removeCallbacks(updateNotificationTask)
        publishHandler.postDelayed(updateNotificationTask, UPDATE_DELAY)
    }

    /**
     * Satisfy a request to publish a notification, updating its content if already visible or
     * showing it otherwise.
     *
     * @param state The current playback state
     * @param metadata The metadata of the currently playing track
     * @param publisher An action to take when the notification is created. This action should
     * take care of publishing the newly created notification.
     */
    private inline fun publishNotification(
        state: PlaybackStateCompat, metadata: MediaMetadataCompat,
        publisher: (Notification) -> Unit
    ) {

        if (state.state == PlaybackStateCompat.STATE_NONE
            || state.state == PlaybackStateCompat.STATE_STOPPED) {
            // Do not show a notification if playback is stopped.
            Log.i(TAG, "No playback state. Clear notification.")
            stop(clearNotification = true)
            return
        }

        val builder = NotificationCompat.Builder(service, CHANNEL_ID)
        configureNotification(builder, state, metadata)
        val notification = builder.build()
        publisher(notification)
    }

    /**
     * Configure the newly displayed or updated notification
     * based on the current playback state and metadata.
     */
    private fun configureNotification(
        builder: NotificationCompat.Builder,
        state: PlaybackStateCompat, metadata: MediaMetadataCompat
    ) {

        val currentMedia = metadata.description
        val isPlaying = state.state == PlaybackStateCompat.STATE_PLAYING

        val mediaStyle = MediaStyle().setMediaSession(service.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)
            // For backwards compatibility with Android L and earlier
            .setShowCancelButton(true)
            .setCancelButtonIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    service,
                    PlaybackStateCompat.ACTION_STOP
                )
            )

        builder.setSmallIcon(if (isPlaying) R.drawable.notif_play_arrow else R.drawable.notif_pause)
            // Pending intent that is fired when user clicks on notification
            .setContentIntent(contentIntent)
            // Title - usually Song name.
            .setContentTitle(currentMedia.title)
            // Subtitle - usually Artist name.
            .setContentText(currentMedia.subtitle)
            .setLargeIcon(currentMedia.iconBitmap ?: defaultLargeIcon)
            // When notification is deleted fire an ACTION_STOP event.
            .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    service,
                    PlaybackStateCompat.ACTION_STOP
                )
            )
            // Show controls on lock screen event when user hides sensitive content.
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(mediaStyle)

        val canSkipToPrev = (state.actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L)
        val canSkipToNext = (state.actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L)

        // Add actions to control playback - skip previous, next and play/pause
        builder.addAction(if (canSkipToPrev) previousAction else emptyAction)
        builder.addAction(if (isPlaying) pauseAction else playAction)
        builder.addAction(if (canSkipToNext) nextAction else emptyAction)

        // Make the notification un-dismissible while playing
        builder.setOngoing(isPlaying)

        // Display current playback position as a chronometer
        if (isPlaying && state.position >= 0) {
            builder.setWhen(System.currentTimeMillis() - state.position)
                .setUsesChronometer(true)
                .setShowWhen(true)
        } else {
            builder.setWhen(0)
                .setUsesChronometer(false)
                .setShowWhen(false)
        }
    }

    /**
     * Listens for changes in the media controller's connection and state
     * to automatically update the content of the notification.
     */
    private inner class ControllerCallback : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            when (state.state) {
                PlaybackStateCompat.STATE_NONE,
                PlaybackStateCompat.STATE_STOPPED -> stop(clearNotification = true)

                PlaybackStateCompat.STATE_PAUSED,
                PlaybackStateCompat.STATE_PLAYING -> {
                    scheduleNotificationUpdate()
                }

                PlaybackStateCompat.STATE_ERROR -> Log.w(TAG, "STATE_ERROR in notification")
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            scheduleNotificationUpdate()
        }
    }
}