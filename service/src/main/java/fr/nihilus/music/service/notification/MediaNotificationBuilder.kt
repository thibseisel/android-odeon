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

package fr.nihilus.music.service.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import fr.nihilus.music.service.MusicService
import fr.nihilus.music.service.R
import fr.nihilus.music.service.ServiceScoped
import fr.nihilus.music.service.extensions.*
import javax.inject.Inject

private const val NOW_PLAYING_CHANNEL = "fr.nihilus.music.media.NOW_PLAYING"
internal const val NOW_PLAYING_NOTIFICATION = 0x1ee7

/**
 * Encapsulate code for building media notifications displaying the currently playing media.
 */
@ServiceScoped
internal class MediaNotificationBuilder @Inject constructor(
    private val context: MusicService,
    session: MediaSessionCompat
) {

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val controller = MediaControllerCompat(context, session.sessionToken)

    /**
     * An action that does nothing.
     * Used to display a blank space in lieu of a disabled action.
     */
    private val noOpAction = NotificationCompat.Action(R.drawable.svc_ic_blank_24, null, null)

    private val previousAction = NotificationCompat.Action(
        R.drawable.svc_ic_skip_previous_36dp,
        context.getString(R.string.svc_action_previous),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_SKIP_TO_PREVIOUS)
    )

    private val playAction = NotificationCompat.Action(
        R.drawable.svc_ic_play_arrow_48dp,
        context.getString(R.string.svc_action_play),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_PLAY)
    )

    private val pauseAction = NotificationCompat.Action(
        R.drawable.svc_ic_pause_48dp,
        context.getString(R.string.svc_action_pause),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_PAUSE)
    )

    private val nextAction = NotificationCompat.Action(
        R.drawable.svc_ic_skip_next_36dp,
        context.getString(R.string.svc_action_next),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_SKIP_TO_NEXT)
    )

    private val stopPendingIntent: PendingIntent =
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_STOP)

    fun buildNotification(): Notification {
        if (shouldCreateNowPlayingChannel()) {
            createNowPlayingChannel()
        }

        val metadata = controller.metadata
        val playbackState = controller.playbackState

        val isPlaying = playbackState.isPlaying

        // Display notification actions depending on playback state and actions availability
        val builder = NotificationCompat.Builder(context, NOW_PLAYING_CHANNEL)
        builder.addAction(if (playbackState.isSkipToPreviousEnabled) previousAction else noOpAction)
        if (isPlaying) {
            builder.addAction(pauseAction)
        } else if (playbackState.isPlayEnabled) {
            builder.addAction(playAction)
        }
        builder.addAction(if (playbackState.isSkipToNextEnabled) nextAction else noOpAction)

        // Display current playback position as a chronometer on Android 9 and older.
        // On Android 10 and onwards a progress bar is already displayed in the notification.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (isPlaying && playbackState.position >= 0) {
                builder.setWhen(System.currentTimeMillis() - playbackState.position)
                    .setUsesChronometer(true)
                    .setShowWhen(true)
            } else {
                builder.setWhen(0)
                    .setUsesChronometer(false)
                    .setShowWhen(false)
            }
        }

        // Specific style for media playback notifications
        val mediaStyle = MediaStyle()
            .setCancelButtonIntent(stopPendingIntent)
            .setMediaSession(controller.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)
            .setShowCancelButton(true)

        return builder.setContentIntent(controller.sessionActivity)
            .setContentTitle(metadata.displayTitle)
            .setContentText(metadata.displaySubtitle)
            .setDeleteIntent(stopPendingIntent)
            .setLargeIcon(metadata.albumArt)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setSmallIcon(if (isPlaying) R.drawable.svc_notif_play_arrow else R.drawable.svc_notif_pause)
            .setStyle(mediaStyle)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun shouldCreateNowPlayingChannel(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !nowPlayingChannelExists()

    @RequiresApi(Build.VERSION_CODES.O)
    private fun nowPlayingChannelExists(): Boolean =
        notificationManager.getNotificationChannel(NOW_PLAYING_CHANNEL) != null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNowPlayingChannel() {
        val notificationChannel = NotificationChannel(
            NOW_PLAYING_CHANNEL,
            context.getString(R.string.svc_channel_mediasession),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.svc_channel_mediasession_description)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            setShowBadge(false)
        }

        notificationManager.createNotificationChannel(notificationChannel)
    }
}