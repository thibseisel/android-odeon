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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.PlaybackStateCompat.*
import fr.nihilus.music.media.R
import fr.nihilus.music.media.di.ServiceScoped
import javax.inject.Inject

private const val NOW_PLAYING_CHANNEL = "fr.nihilus.music.media.NOW_PLAYING"

@ServiceScoped
internal class MediaNotificationBuilder
@Inject constructor(private val context: MusicService) {

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val skipToPreviousAction = NotificationCompat.Action(
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

    private val skipToNextAction = NotificationCompat.Action(
        R.drawable.abc_ic_skip_next_36dp,
        context.getString(R.string.abc_action_next),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_SKIP_TO_NEXT)
    )

    private val stopPendingIntent: PendingIntent =
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_STOP)

    private val notificationChannelName = context.getString(R.string.abc_channel_mediasession)
    private val notificationChannelDescription = context.getString(R.string.abc_channel_mediasession_description)

    @RequiresApi(Build.VERSION_CODES.O)
    private fun nowPlayingChannelExists(): Boolean =
        notificationManager.getNotificationChannel(NOW_PLAYING_CHANNEL) != null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNowPlayingChannel() {
        val notificationChannel = NotificationChannel(NOW_PLAYING_CHANNEL,
            notificationChannelName,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = notificationChannelDescription
        }

        notificationManager.createNotificationChannel(notificationChannel)
    }
}