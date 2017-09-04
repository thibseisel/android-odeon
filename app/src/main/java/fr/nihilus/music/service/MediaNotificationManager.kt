package fr.nihilus.music.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Build
import android.os.RemoteException
import android.support.annotation.DrawableRes
import android.support.v4.app.NotificationCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import fr.nihilus.music.HomeActivity
import fr.nihilus.music.R
import fr.nihilus.music.asMediaDescription
import fr.nihilus.music.di.ServiceScoped
import javax.inject.Inject

@ServiceScoped
class MediaNotificationManager
@Inject constructor(service: MusicService) : BroadcastReceiver() {

    private val mService = service

    private val mNotificationManager: NotificationManager
    private val mPauseIntent: PendingIntent
    private val mPlayIntent: PendingIntent
    private val mPreviousIntent: PendingIntent
    private val mNextIntent: PendingIntent

    private var mController: MediaControllerCompat? = null
    private var mTransportControls: MediaControllerCompat.TransportControls? = null

    private var mSessionToken: MediaSessionCompat.Token? = null
    private var mPlaybackState: PlaybackStateCompat? = null

    private var mMetadata: MediaMetadataCompat? = null
    private var mStarted = false

    init {
        val pkg = service.packageName
        mPauseIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
                Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT)
        mPlayIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
                Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT)
        mPreviousIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
                Intent(ACTION_PREVIOUS).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT)
        mNextIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
                Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT)

        // Cancel all notifications to handle the case where the Service was killed
        // and restarted by the system.
        mNotificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        initNotificationChannel()
        mNotificationManager.cancelAll()
    }

    fun init() {
        updateSessionToken()
    }

    private fun initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "MusicPlayer control"
            val channel = NotificationChannel(CHANNEL_ID, channelName,
                    NotificationManager.IMPORTANCE_LOW)
            mNotificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateSessionToken() {
        val freshToken = mService.sessionToken
        if (mSessionToken == null && freshToken != null ||
                mSessionToken != null && mSessionToken != freshToken) {
            mController?.unregisterCallback(mControllerCallback)
            mSessionToken = freshToken
            if (mSessionToken != null) {
                mController = MediaControllerCompat(mService, mSessionToken!!)
                mTransportControls = mController!!.transportControls
                if (mStarted) {
                    mController!!.registerCallback(mControllerCallback)
                }
            }
        }
    }

    fun startNotification() {
        Log.d(TAG, "startingNotification called")
        mMetadata = mController!!.metadata
        mPlaybackState = mController!!.playbackState

        val notification = createNotification()
        if (notification != null) {
            mController!!.registerCallback(mControllerCallback)
            val filter = IntentFilter()
            filter.addAction(ACTION_PAUSE)
            filter.addAction(ACTION_PLAY)
            filter.addAction(ACTION_PREVIOUS)
            filter.addAction(ACTION_NEXT)
            mService.registerReceiver(this, filter)

            Log.i(TAG, "FOREGROUND: true")
            mService.startForeground(NOTIFICATION_ID, notification)
            mStarted = true
        }
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        val action = intent.action
        Log.v(TAG, "Received intent with action $action")
        when (action) {
            ACTION_PAUSE -> mTransportControls!!.pause()
            ACTION_PLAY -> mTransportControls!!.play()
            ACTION_PREVIOUS -> mTransportControls!!.skipToPrevious()
            ACTION_NEXT -> mTransportControls!!.skipToNext()
            else -> Log.w(TAG, "Unknown intent ignored. Action=$action")
        }
    }

    private val mControllerCallback = object : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            mPlaybackState = state
            Log.v(TAG, "Received new playback state $state")
            if (state.state == PlaybackStateCompat.STATE_STOPPED ||
                    state.state == PlaybackStateCompat.STATE_NONE) {
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

    private fun createContentIntent(): PendingIntent {
        val openUi = Intent(mService, HomeActivity::class.java)
        openUi.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(mService, REQUEST_CODE, openUi,
                PendingIntent.FLAG_CANCEL_CURRENT)
    }

    private fun createNotification(): Notification? {
        Log.v(TAG, "updateNotificationMetadata: metadata=$mMetadata")
        if (mMetadata == null || mPlaybackState == null) return null

        val notificationBuilder = NotificationCompat.Builder(mService, CHANNEL_ID)
        notificationBuilder.addAction(R.drawable.ic_skip_previous_24dp,
                mService.getString(R.string.action_previous), mPreviousIntent)

        addPlayPauseButton(notificationBuilder)

        notificationBuilder.addAction(R.drawable.ic_skip_next_24dp,
                mService.getString(R.string.action_next), mNextIntent)

        val description = mMetadata!!.asMediaDescription(MediaDescriptionCompat.Builder())

        val smallIcon = if (mPlaybackState!!.state == PlaybackStateCompat.STATE_PLAYING)
            R.drawable.ic_play_arrow else R.drawable.ic_pause

        notificationBuilder.setStyle(android.support.v4.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1)
                .setMediaSession(mSessionToken))
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setOnlyAlertOnce(true)
                .setSmallIcon(smallIcon)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setUsesChronometer(true)
                .setContentIntent(createContentIntent())
                .setContentTitle(description.title)
                .setContentText(description.subtitle)

        Glide.with(mService)
                .load(description.iconUri).asBitmap()
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(art: Bitmap?, anim: GlideAnimation<in Bitmap>?) {
                        val notif = notificationBuilder.setLargeIcon(art).build()
                        if (notif != null) {
                            mNotificationManager.notify(NOTIFICATION_ID, notif)
                        }
                    }
                })

        setNotificationPlaybackState(notificationBuilder)
        return notificationBuilder.build()
    }

    private fun addPlayPauseButton(builder: NotificationCompat.Builder) {
        Log.v(TAG, "updatePlayPauseAction")
        val label: String
        val intent: PendingIntent
        @DrawableRes val icon: Int

        if (mPlaybackState!!.state == PlaybackStateCompat.STATE_PLAYING) {
            label = "Pause"
            icon = R.drawable.ic_pause
            intent = mPauseIntent
        } else {
            label = "Play"
            icon = R.drawable.ic_play_arrow
            intent = mPlayIntent
        }

        builder.addAction(icon, label, intent)
    }

    private fun setNotificationPlaybackState(builder: NotificationCompat.Builder) {
        Log.v(TAG, "updateNotificationPlaybackState: mPlaybackState=$mPlaybackState")
        if (mPlaybackState == null || !mStarted) {
            Log.v(TAG, "updatedNotificationPlaybackState: canceling notification!")
            mService.stopForeground(true)
            return
        }

        if (mPlaybackState!!.state == PlaybackStateCompat.STATE_PLAYING
                && mPlaybackState!!.position >= 0) {
            builder.setWhen(System.currentTimeMillis() - mPlaybackState!!.position)
                    .setShowWhen(true)
                    .setUsesChronometer(true)
        } else {
            Log.v(TAG, "updateNotificationPlaybackState: hiding playback position")
            builder.setWhen(0)
                    .setShowWhen(false)
                    .setUsesChronometer(false)
        }

        // Make sure that the notification can be dismissed by the user when we are not playing
        builder.setOngoing(mPlaybackState!!.state == PlaybackStateCompat.STATE_PLAYING)
    }

    fun stopNotification() {
        if (mStarted) {
            Log.d(TAG, "stopNotification called: hiding notification")
            mStarted = false
            mController!!.unregisterCallback(mControllerCallback)
            try {
                mNotificationManager.cancel(NOTIFICATION_ID)
                mService.unregisterReceiver(this)
            } catch (e: IllegalArgumentException) {
                // Ignore if the receiver is not registered.
            } finally {
                Log.d(TAG, "Stopping foreground state")
                mService.stopForeground(true)
            }
        }
    }

    companion object {
        private const val TAG = "MediaNotifMgr"
        const val REQUEST_CODE = 100
        const val NOTIFICATION_ID = 42
        const val ACTION_PLAY = "fr.nihilus.music.action.PLAY"
        const val ACTION_PAUSE = "fr.nihilus.music.action.PAUSE"
        const val ACTION_PREVIOUS = "fr.nihilus.music.action.PREVIOUS"
        const val ACTION_NEXT = "fr.nihilus.music.action.NEXT"
        const val CHANNEL_ID = "media_channel"
    }
}