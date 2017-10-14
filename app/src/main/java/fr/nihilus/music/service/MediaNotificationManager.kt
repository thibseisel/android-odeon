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
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import fr.nihilus.music.HomeActivity
import fr.nihilus.music.R
import fr.nihilus.music.asMediaDescription
import fr.nihilus.music.di.ServiceScoped
import javax.inject.Inject

@ServiceScoped
class MediaNotificationManager
@Inject constructor(private val service: MusicService) {

    private val mNotificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

    private val mPauseAction = NotificationCompat.Action(R.drawable.ic_pause_48dp,
            service.getString(R.string.action_pause),
            MediaButtonReceiver.buildMediaButtonPendingIntent(service,
                    PlaybackStateCompat.ACTION_PAUSE))

    private val mPlayAction = NotificationCompat.Action(R.drawable.ic_play_arrow_48dp,
            service.getString(R.string.action_play),
            MediaButtonReceiver.buildMediaButtonPendingIntent(service,
                    PlaybackStateCompat.ACTION_PLAY))

    private val mPreviousAction = NotificationCompat.Action(R.drawable.ic_skip_previous_36dp,
            service.getString(R.string.action_previous), MediaButtonReceiver.buildMediaButtonPendingIntent(service,
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))

    private val mNextAction = NotificationCompat.Action(R.drawable.ic_skip_next_36dp,
            service.getString(R.string.action_next),
            MediaButtonReceiver.buildMediaButtonPendingIntent(service,
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT))

    private val mControllerCallback = ControllerCallback()

    private var mController: MediaControllerCompat? = null
    private var mTransportControls: MediaControllerCompat.TransportControls? = null

    private var mSessionToken: MediaSessionCompat.Token? = null
    private var mPlaybackState: PlaybackStateCompat? = null

    private var mMetadata: MediaMetadataCompat? = null
    private var mStarted = false

    fun init() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }
        // Cancel all notifications to handle the case where the Service was killed
        // and restarted by the system.
        mNotificationManager.cancelAll()
        updateSessionToken()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        if (mNotificationManager.getNotificationChannel(CHANNEL_ID) == null) {

            val name = service.getString(R.string.channel_mediasession)
            val channelDescription = service.getString(R.string.channel_mediasession_description)
            val importance = NotificationManager.IMPORTANCE_LOW

            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            with(channel) {
                description = channelDescription
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
                vibrationPattern = longArrayOf(100L, 200L, 300L, 400L, 500L, 400L, 300L, 200L, 400L)
            }

            mNotificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateSessionToken() {
        val freshToken = service.sessionToken
        if (mSessionToken == null && freshToken != null ||
                mSessionToken != null && mSessionToken != freshToken) {
            mController?.unregisterCallback(mControllerCallback)
            mSessionToken = freshToken
            if (mSessionToken != null) {
                mController = MediaControllerCompat(service, mSessionToken!!)
                mTransportControls = mController!!.transportControls
                if (mStarted) {
                    mController!!.registerCallback(mControllerCallback)
                }
            }
        }
    }

    fun startNotification() {
        mMetadata = mController!!.metadata
        mPlaybackState = mController!!.playbackState

        val notification = createNotification()
        if (notification != null) {
            mController!!.registerCallback(mControllerCallback)

            Log.i(TAG, "FOREGROUND: true")
            service.startForeground(NOTIFICATION_ID, notification)
            mStarted = true
        }
    }

    private fun createContentIntent(): PendingIntent {
        val openUi = Intent(service, HomeActivity::class.java)
        openUi.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(service, REQUEST_CODE, openUi,
                PendingIntent.FLAG_CANCEL_CURRENT)
    }

    private fun createNotification(): Notification? {
        Log.v(TAG, "updateNotificationMetadata: metadata=$mMetadata")
        if (mMetadata == null || mPlaybackState == null) return null

        val notificationBuilder = NotificationCompat.Builder(service, CHANNEL_ID)

        val description = mMetadata!!.asMediaDescription(MediaDescriptionCompat.Builder())

        val smallIcon = if (mPlaybackState!!.state == PlaybackStateCompat.STATE_PLAYING)
            R.drawable.ic_play_arrow else R.drawable.ic_pause

        notificationBuilder.setStyle(android.support.v4.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mSessionToken)
                .setShowActionsInCompactView(0, 1, 2)
                // For backwards compatibility with Android L and earlier
                .setShowCancelButton(true)
                .setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                service, PlaybackStateCompat.ACTION_STOP)))
                .setSmallIcon(smallIcon)
                // Pending intent that is fired when user clicks on notification
                .setContentIntent(createContentIntent())
                // Title - usually Song name.
                .setContentTitle(description.title)
                // Subtitle - usually Artist name.
                .setContentText(description.subtitle)
                .setLargeIcon(description.iconBitmap)
                // When notification is deleted (when playback is paused and notification
                // can be deleted) fire MediaButtonPendingIntent with ACTION_STOP.
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(
                        service, PlaybackStateCompat.ACTION_STOP))
                // Show controls on lock screen event when user hides sensitive content.
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        notificationBuilder.addAction(mPreviousAction)
        addPlayPauseButton(notificationBuilder)
        notificationBuilder.addAction(mNextAction)

        setNotificationPlaybackState(notificationBuilder)
        return notificationBuilder.build()
    }

    private fun addPlayPauseButton(builder: NotificationCompat.Builder) {
        builder.addAction(
                if (mPlaybackState!!.state == PlaybackStateCompat.STATE_PLAYING) mPauseAction
                else mPlayAction
        )
    }

    private fun setNotificationPlaybackState(builder: NotificationCompat.Builder) {
        if (mPlaybackState == null || !mStarted) {
            service.stopForeground(true)
            return
        }

        if (mPlaybackState!!.state == PlaybackStateCompat.STATE_PLAYING
                && mPlaybackState!!.position >= 0) {
            builder.setWhen(System.currentTimeMillis() - mPlaybackState!!.position)
                    .setShowWhen(true)
                    .setUsesChronometer(true)
        } else {
            builder.setWhen(0)
                    .setShowWhen(false)
                    .setUsesChronometer(false)
        }

        // Make sure that the notification can be dismissed by the user when we are not playing
        builder.setOngoing(mPlaybackState!!.state == PlaybackStateCompat.STATE_PLAYING)
    }

    fun stopNotification() {
        if (mStarted) {
            mStarted = false
            mController!!.unregisterCallback(mControllerCallback)
            try {
                mNotificationManager.cancel(NOTIFICATION_ID)
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

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            mPlaybackState = state
            Log.v(TAG, "Received new playback state $state")

            if (state == null
                    || state.state == PlaybackStateCompat.STATE_STOPPED
                    || state.state == PlaybackStateCompat.STATE_NONE) {
                stopNotification()
            } else {
                val notification = createNotification()
                if (notification != null) {
                    mNotificationManager.notify(NOTIFICATION_ID, notification)
                }
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            mMetadata = metadata
            Log.v(TAG, "Received new metadata $metadata")

            val bitmap = metadata?.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)

            val notification = createNotification()
            if (notification != null) {
                mNotificationManager.notify(NOTIFICATION_ID, notification)
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