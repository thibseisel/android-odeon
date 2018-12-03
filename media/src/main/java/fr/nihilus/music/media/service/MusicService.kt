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

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import dagger.android.AndroidInjection
import fr.nihilus.music.media.*
import fr.nihilus.music.media.repo.MusicRepository
import fr.nihilus.music.media.usage.MediaUsageManager
import fr.nihilus.music.media.utils.PermissionDeniedException
import fr.nihilus.music.media.utils.plusAssign
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class MusicService : MediaBrowserServiceCompat(), CoroutineScope {

    @Inject internal lateinit var repository: MusicRepository
    @Inject internal lateinit var notificationBuilder: MediaNotificationBuilder
    @Inject internal lateinit var usageManager: MediaUsageManager

    @Inject internal lateinit var session: MediaSessionCompat
    @Inject internal lateinit var connector: MediaSessionConnector
    @Inject internal lateinit var player: Player
    @Inject internal lateinit var settings: MediaSettings
    @Inject internal lateinit var subscriptions: CompositeDisposable

    private lateinit var mediaController: MediaControllerCompat
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var becomingNoisyReceiver: BecomingNoisyReceiver
    private lateinit var packageValidator: PackageValidator

    private lateinit var scopeJob: Job
    override val coroutineContext: CoroutineContext
        get() = scopeJob + Dispatchers.Main

    private val controllerCallback = MediaControllerCallback()
    private val serviceStopper = ServiceStopper(this)

    private var isForegroundService = false
    private var isStarted = false

    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)

        scopeJob = Job()

        // Make the media session discoverable and able to receive commands.
        session.isActive = true

        /**
         * In order for [MediaBrowserCompat.ConnectionCallback.onConnected] to be called,
         * a [MediaSessionCompat.Token] needs to be set on the [MediaBrowserServiceCompat].
         *
         * It is possible to wait to set the session token, if required for a specific use-case.
         * However, the token *must* be set by the time [MediaBrowserServiceCompat.onGetRoot]
         * returns, or the connection will fail silently. (The system will not even call
         * [MediaBrowserCompat.ConnectionCallback.onConnectionFailed].)
         */
        sessionToken = session.sessionToken

        // Restore shuffle and repeat mode for the media session and the player (through callbacks)
        mediaController = MediaControllerCompat(this, session)
        mediaController.transportControls.run {
            setShuffleMode(settings.shuffleMode)
            setRepeatMode(settings.repeatMode)
        }

        // Because ExoPlayer will manage the MediaSession, add the service as a callback for state changes.
        mediaController.registerCallback(controllerCallback)

        // Listen to track completion events
        val completionListener = TrackCompletionListener()
        player.addListener(completionListener)

        notificationManager = NotificationManagerCompat.from(this)
        becomingNoisyReceiver = BecomingNoisyReceiver(this, session.sessionToken)
        packageValidator = PackageValidator(this, R.xml.abc_allowed_media_browser_callers)

        // Listen for changes in the repository to notify media browsers.
        // If the changed media ID is a track, notify for its parent category.
        subscriptions += repository.getMediaChanges()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { browseCategoryOf(it) }
            .subscribe(this::notifyChildrenChanged)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        isStarted = true
        return Service.START_NOT_STICKY
    }

    override fun onDestroy() {
        Timber.i("Destroying service.")

        session.run {
            isActive = false
            release()
        }

        // Clear all subscriptions to prevent resource leaks
        subscriptions.clear()
        scopeJob.cancel()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): MediaBrowserServiceCompat.BrowserRoot? {
        /**
         * Allow connections to the MediaBrowserService in debug builds, otherwise
         * check the caller's signature and disconnect it if not allowed by returning `null`.
         */
        return if (BuildConfig.DEBUG || packageValidator.isCallerAllowed(clientPackageName, clientUid)) {
            BrowserRoot(BROWSER_ROOT, null)
        } else null
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        onLoadChildren(parentId, result, Bundle.EMPTY)
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>, options: Bundle) {
        Timber.v("Start loading children for ID: %s", parentId)
        result.detach()

        subscriptions += repository.getMediaItems(parentId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { items: List<MediaBrowserCompat.MediaItem> ->
                    Timber.v("Loaded items for %s: size=%d", parentId, items.size)
                    result.sendResult(items)
                },
                { error: Throwable ->
                    when (error) {
                        is UnsupportedOperationException -> Timber.w("Unsupported parent id: %s", parentId)
                        is PermissionDeniedException -> Timber.i(error)
                        else -> throw error
                    }

                    result.sendResult(null)
                }
            )
    }

    override fun notifyChildrenChanged(parentId: String) {
        repository.clear()
        super.notifyChildrenChanged(parentId)
    }

    internal fun stopService() {
        stopSelf()
        isStarted = false
    }

    /**
     * Receive callbacks about state changes to the [MediaSessionCompat].
     * In response to those callbacks, this class:
     *
     * - Build/update the service's notification.
     * - Register/unregister a broadcast receiver for [AudioManager.ACTION_AUDIO_BECOMING_NOISY].
     * - Calls [MusicService.startForeground] and [MusicService.stopForeground].
     * - Save changes to shuffle mode and repeat mode to settings.
     */
    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            mediaController.playbackState?.let { updateNotification(it) }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            state?.let { updateNotification(it) }
        }

        override fun onShuffleModeChanged(shuffleMode: Int) {
            settings.shuffleMode = shuffleMode
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            settings.repeatMode = repeatMode
        }

        private fun updateNotification(state: PlaybackStateCompat) {
            val updatedState = state.state
            if (mediaController.metadata == null) {
                // Do not update notification when no metadata.
                return
            }

            // Skip building a notification when state is NONE.
            val notification = if (updatedState != PlaybackStateCompat.STATE_NONE) {
                notificationBuilder.buildNotification()
            } else null

            when (updatedState) {

                PlaybackStateCompat.STATE_BUFFERING,
                PlaybackStateCompat.STATE_PLAYING -> {
                    becomingNoisyReceiver.register()
                    startForeground(NOW_PLAYING_NOTIFICATION, notification)
                    isForegroundService = true

                    if (!isStarted) {
                        // Start service to keep it running while playing.
                        startService(Intent(this@MusicService, MusicService::class.java))
                    } else {
                        // Cancel the scheduled stop of the service.
                        serviceStopper.cancel()
                    }
                }

                else -> {
                    becomingNoisyReceiver.unregister()

                    if (isForegroundService) {
                        stopForeground(false)
                        if (notification != null) {
                            notificationManager.notify(NOW_PLAYING_NOTIFICATION, notification)
                        } else {
                            stopForeground(true)
                        }

                        isForegroundService = false
                    }

                    // If the service is started, schedule to stop it automatically at a later time.
                    if (isStarted) {
                        serviceStopper.schedule(30L, TimeUnit.SECONDS)
                    }
                }
            }
        }

        override fun onSessionDestroyed() {
            player.run {
                stop()
                release()
            }
        }
    }

    private inner class TrackCompletionListener : Player.EventListener {
        private val windowBuffer = Timeline.Window()

        override fun onPositionDiscontinuity(@Player.DiscontinuityReason reason: Int) {
            if (reason == Player.DISCONTINUITY_REASON_PERIOD_TRANSITION) {
                onTrackCompletion(player)
            }
        }

        private fun onTrackCompletion(player: Player) {
            val completedTrackIndex = player.previousWindowIndex
            if (completedTrackIndex == C.INDEX_UNSET) {
                Timber.w("Attempt to retrieve information of a track that completed playback, but previous index is unset.")
                return
            }

            player.currentTimeline.getWindow(completedTrackIndex, windowBuffer, true)
            val completedMedia = windowBuffer.tag as? MediaDescriptionCompat
            val completedTrackId = musicIdFrom(completedMedia?.mediaId)

            if (completedTrackId != null) {
                usageManager.reportCompletion(completedTrackId)
            }
        }
    }
}

/**
 * A receiver that listens for when headphones are unplugged.
 * This pauses playback to prevent it from being noisy.
 */
private class BecomingNoisyReceiver(
    private val context: Context,
    sessionToken: MediaSessionCompat.Token
) : BroadcastReceiver() {

    private val noisyIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private val controller = MediaControllerCompat(context, sessionToken)

    private var isRegistered = false

    fun register() {
        if (!isRegistered) {
            context.registerReceiver(this, noisyIntentFilter)
            isRegistered = true
        }
    }

    fun unregister() {
        if (isRegistered) {
            context.unregisterReceiver(this)
            isRegistered = false
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            controller.transportControls.pause()
        }
    }
}

private class ServiceStopper(service: MusicService) {
    private val serviceRef = WeakReference<MusicService>(service)
    private val handler = Handler()

    private var scheduled = false

    private val stopServiceTask = Runnable {
        serviceRef.get()?.stopService()
    }

    fun schedule(time: Long, unit: TimeUnit) {
        if (!scheduled) {
            handler.postDelayed(stopServiceTask, unit.toMillis(time))
            scheduled = true
        }
    }

    fun cancel() {
        if (scheduled) {
            handler.removeCallbacks(stopServiceTask)
            scheduled = false
        }
    }
}
