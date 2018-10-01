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

package fr.nihilus.music.media.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.media.app.NotificationCompat.MediaStyle
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import fr.nihilus.music.media.R
import fr.nihilus.music.media.assert
import fr.nihilus.music.media.di.ServiceScoped
import fr.nihilus.music.media.extensions.isPlayEnabled
import fr.nihilus.music.media.extensions.isPlaying
import fr.nihilus.music.media.extensions.isSkipToNextEnabled
import fr.nihilus.music.media.extensions.isSkipToPreviousEnabled
import javax.inject.Inject

internal const val NOW_PLAYING_CHANNEL = "fr.nihilus.music.media.NOW_PLAYING"
internal const val NOW_PLAYING_NOTIFICATION = 0x1ee7

/**
 * Encapsulate code for building media notifications displaying the currently playing media.
 */
@ServiceScoped
internal class MediaNotificationBuilder
@Inject constructor(
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
    private val noOpAction = NotificationCompat.Action(0, null, null)

    private val rewindAction = NotificationCompat.Action(
        R.drawable.abc_ic_fast_rewind_36dp,
        context.getString(R.string.abc_action_rewind),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_REWIND)
    )

    private val previousAction = NotificationCompat.Action(
        R.drawable.abc_ic_skip_previous_36dp,
        context.getString(R.string.abc_action_previous),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_SKIP_TO_PREVIOUS)
    )

    private val playAction = NotificationCompat.Action(
        R.drawable.abc_ic_play_arrow_48dp,
        context.getString(R.string.abc_action_play),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_PLAY)
    )

    private val pauseAction = NotificationCompat.Action(
        R.drawable.abc_ic_pause_48dp,
        context.getString(R.string.abc_action_pause),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_PAUSE)
    )

    private val nextAction = NotificationCompat.Action(
        R.drawable.abc_ic_skip_next_36dp,
        context.getString(R.string.abc_action_next),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_SKIP_TO_NEXT)
    )

    private val fastForwardAction = NotificationCompat.Action(
        R.drawable.abc_ic_fast_forward_36dp,
        context.getString(R.string.abc_action_fast_forward),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_FAST_FORWARD)
    )

    private val stopPendingIntent: PendingIntent =
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_STOP)

    fun buildNotification(): Notification {
        if (shouldCreateNowPlayingChannel()) {
            createNowPlayingChannel()
        }

        assert(controller.metadata != null)
        assert(controller.playbackState != null)
        val description =  controller.metadata.description
        val playbackState = controller.playbackState

        val isPlaying = playbackState.isPlaying

        // Display notification actions depending on playback state and actions availability
        val builder = NotificationCompat.Builder(context, NOW_PLAYING_CHANNEL)
        builder.addAction(if (playbackState.isSkipToPreviousEnabled) previousAction else noOpAction)
        builder.addAction(rewindAction)
        if (isPlaying) {
            builder.addAction(pauseAction)
        } else if (playbackState.isPlayEnabled) {
            builder.addAction(playAction)
        }
        builder.addAction(fastForwardAction)
        builder.addAction(if (playbackState.isSkipToNextEnabled) nextAction else noOpAction)

        // Display current playback position as a chronometer
        if (isPlaying && playbackState.position >= 0) {
            builder.setWhen(System.currentTimeMillis() - playbackState.position)
                .setUsesChronometer(true)
                .setShowWhen(true)
        } else {
            builder.setWhen(0)
                .setUsesChronometer(false)
                .setShowWhen(false)
        }

        // Specific style for media playback notifications
        val mediaStyle = MediaStyle()
            .setCancelButtonIntent(stopPendingIntent)
            .setMediaSession(controller.sessionToken)
            .setShowActionsInCompactView(0, 2, 4)
            .setShowCancelButton(true)

        return builder.setContentIntent(controller.sessionActivity)
            .setContentTitle(description.title)
            .setContentText(description.subtitle)
            .setDeleteIntent(stopPendingIntent)
            .setLargeIcon(description.iconBitmap)
            .setOnlyAlertOnce(true)
            .setSmallIcon(if (isPlaying) R.drawable.abc_notif_play_arrow else R.drawable.abc_notif_pause)
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
        val notificationChannel = NotificationChannel(NOW_PLAYING_CHANNEL,
            context.getString(R.string.abc_channel_mediasession),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.abc_channel_mediasession_description)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }

        notificationManager.createNotificationChannel(notificationChannel)
    }
}