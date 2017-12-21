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
import android.os.RemoteException
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.app.NotificationCompat.MediaStyle
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
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

    private val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

    private val pauseAction = NotificationCompat.Action(R.drawable.ic_pause_48dp,
            service.getString(R.string.action_pause),
            MediaButtonReceiver.buildMediaButtonPendingIntent(service,
                    PlaybackStateCompat.ACTION_PAUSE))

    private val playAction = NotificationCompat.Action(R.drawable.ic_play_arrow_48dp,
            service.getString(R.string.action_play),
            MediaButtonReceiver.buildMediaButtonPendingIntent(service,
                    PlaybackStateCompat.ACTION_PLAY))

    private val previousAction = NotificationCompat.Action(R.drawable.ic_skip_previous_36dp,
            service.getString(R.string.action_previous), MediaButtonReceiver.buildMediaButtonPendingIntent(service,
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))

    private val nextAction = NotificationCompat.Action(R.drawable.ic_skip_next_36dp,
            service.getString(R.string.action_next),
            MediaButtonReceiver.buildMediaButtonPendingIntent(service,
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT))

    private val defaultLargeIcon = loadResourceAsBitmap(service, R.drawable.ic_default_icon,
            320, 320)

    private val contentIntent = PendingIntent.getActivity(
            service, REQUEST_CODE,
            Intent(service, HomeActivity::class.java),
            PendingIntent.FLAG_CANCEL_CURRENT)

    private val controllerCallback = ControllerCallback()

    private var controller: MediaControllerCompat? = null

    private var sessionToken: MediaSessionCompat.Token? = null
    private var playbackState: PlaybackStateCompat? = null

    private var metadata: MediaMetadataCompat? = null
    private var isStarted = false

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }
        // Cancel all notifications to handle the case where the Service was killed
        // and restarted by the system.
        notificationManager.cancelAll()
        updateSessionToken()
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

    private fun updateSessionToken() {
        val freshToken = service.sessionToken
        if (sessionToken == null && freshToken != null ||
                sessionToken != null && sessionToken != freshToken) {
            controller?.unregisterCallback(controllerCallback)
            sessionToken = freshToken
            if (sessionToken != null) {
                controller = MediaControllerCompat(service, sessionToken!!)
                if (isStarted) {
                    controller!!.registerCallback(controllerCallback)
                }
            }
        }
    }

    fun startNotification() {
        metadata = controller!!.metadata
        playbackState = controller!!.playbackState

        val notification = createNotification()
        if (notification != null) {
            controller!!.registerCallback(controllerCallback)

            Log.i(TAG, "FOREGROUND: true")
            service.startForeground(NOTIFICATION_ID, notification)
            isStarted = true
        }
    }

    private fun createNotification(): Notification? {
        val currentMetadata = metadata ?: return null
        val currentState = playbackState ?: return null

        /*Log.d(TAG, """createNotification.
            Metadata = ${currentMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)}
            PlaybackState = ${playbackStates[currentState.state]}
            """.trimIndent())*/

        val notificationBuilder = NotificationCompat.Builder(service, CHANNEL_ID)

        val description = currentMetadata.description

        val smallIcon = if (currentState.state == PlaybackStateCompat.STATE_PLAYING)
            R.drawable.notif_play_arrow else R.drawable.notif_pause

        // Configure this notification for playback control of a MediaSession
        val mediaStyle = MediaStyle()
                .setMediaSession(sessionToken)
                .setShowActionsInCompactView(0, 1, 2)
                // For backwards compatibility with Android L and earlier
                .setShowCancelButton(true)
                .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(service,
                        PlaybackStateCompat.ACTION_STOP))

        notificationBuilder
                .setSmallIcon(smallIcon)
                // Pending intent that is fired when user clicks on notification
                .setContentIntent(contentIntent)
                // Title - usually Song name.
                .setContentTitle(description.title)
                // Subtitle - usually Artist name.
                .setContentText(description.subtitle)
                .setLargeIcon(description.iconBitmap ?: defaultLargeIcon)
                // When notification is deleted fire an ACTION_STOP event.
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(
                        service, PlaybackStateCompat.ACTION_STOP))
                // Show controls on lock screen event when user hides sensitive content.
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(mediaStyle)

        notificationBuilder.addAction(previousAction)
        notificationBuilder.addAction(
                if (currentState.state == PlaybackStateCompat.STATE_PLAYING) pauseAction
                else playAction
        )
        notificationBuilder.addAction(nextAction)

        setNotificationPlaybackState(notificationBuilder)
        return notificationBuilder.build()
    }

    private fun setNotificationPlaybackState(builder: NotificationCompat.Builder) {
        if (playbackState == null || !isStarted) {
            service.stopForeground(true)
            return
        }

        if (playbackState!!.state == PlaybackStateCompat.STATE_PLAYING
                && playbackState!!.position >= 0) {
            builder.setWhen(System.currentTimeMillis() - playbackState!!.position)
                    .setShowWhen(true)
                    .setUsesChronometer(true)
        } else {
            builder.setWhen(0)
                    .setShowWhen(false)
                    .setUsesChronometer(false)
        }

        // Make sure that the notification can be dismissed by the user when we are not playing
        builder.setOngoing(playbackState!!.state == PlaybackStateCompat.STATE_PLAYING)
    }

    fun stopNotification() {
        if (isStarted) {
            isStarted = false
            controller!!.unregisterCallback(controllerCallback)
            try {
                notificationManager.cancel(NOTIFICATION_ID)
            } catch (e: IllegalArgumentException) {
                // Ignore if the receiver is not registered.
            } finally {
                service.stopForeground(true)
            }
        }
    }

    /**
     * Listens for changes in the media controller's connection and state
     * to automatically update the content of the notification.
     */
    private inner class ControllerCallback : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            //logPlaybackState(TAG, state)
            playbackState = state

            when (state.state) {
                PlaybackStateCompat.STATE_NONE,
                PlaybackStateCompat.STATE_STOPPED -> stopNotification()

                PlaybackStateCompat.STATE_PAUSED,
                PlaybackStateCompat.STATE_PLAYING -> {
                    val notification = createNotification()
                    if (notification != null) {
                        //Log.d(TAG, "onPlaybackStateChanged: update notification")
                        notificationManager.notify(NOTIFICATION_ID, notification)
                    }
                }

                PlaybackStateCompat.STATE_ERROR -> Log.w(TAG, "STATE_ERROR in notification")
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            //Log.d(TAG, "onMetadataChanged: title=${metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE)}")
            this@MediaNotificationManager.metadata = metadata

            val notification = createNotification()
            if (notification != null) {
                //Log.d(TAG, "onMetadataChanged: update notification")
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }

        override fun onSessionDestroyed() {
            Log.v(TAG, "Session was destroyed, resetting to the new session token")
            try {
                updateSessionToken()
            } catch (e: RemoteException) {
                Log.e(TAG, "Could not connect to MediaController", e)
            }
        }
    }

    companion object {
        private const val TAG = "MediaNotifMgr"
        const val REQUEST_CODE = 100
        const val NOTIFICATION_ID = 42
        const val CHANNEL_ID = "media_channel"
    }
}