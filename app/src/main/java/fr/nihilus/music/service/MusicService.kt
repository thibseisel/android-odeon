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

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.content.ContextCompat.startForegroundService
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ext.mediasession.RepeatModeActionProvider
import com.google.android.exoplayer2.util.ErrorMessageProvider
import dagger.android.AndroidInjection
import fr.nihilus.music.BuildConfig
import fr.nihilus.music.HomeActivity
import fr.nihilus.music.MediaItemResult
import fr.nihilus.music.doIfPresent
import fr.nihilus.music.media.repo.MusicRepository
import fr.nihilus.music.playback.MediaQueueManager
import fr.nihilus.music.playback.PlaybackController
import fr.nihilus.music.utils.MediaID
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.lang.ref.WeakReference
import javax.inject.Inject

private const val TAG = "MusicService"

/** Number of milliseconds to wait until the service stops itself when not playing. */
private const val STOP_DELAY = 30000L

class MusicService : MediaBrowserServiceCompat() {

    @Inject lateinit var repository: MusicRepository
    @Inject lateinit var notificationMgr: MediaNotificationManager

    @Inject lateinit var player: ExoPlayer
    @Inject lateinit var playbackController: PlaybackController
    @Inject lateinit var queueManager: MediaQueueManager
    @Inject lateinit var errorHandler: ErrorMessageProvider<ExoPlaybackException>

    private lateinit var packageValidator: PackageValidator
    private val delayedStopHandler = DelayedStopHandler(this)
    private val playbackStateListener = PlaybackStateListener()

    private var isStarted = false

    lateinit var session: MediaSessionCompat

    override fun onCreate() {
        super.onCreate()

        session = MediaSessionCompat(this, TAG)
        sessionToken = session.sessionToken

        // Inject dependencies only after MediaSession is instantiated
        AndroidInjection.inject(this)

        val appContext = applicationContext
        val uiIntent = Intent(appContext, HomeActivity::class.java)
        val pi = PendingIntent.getActivity(appContext, 99,
                uiIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        session.setSessionActivity(pi)
        session.setRatingType(RatingCompat.RATING_NONE)

        packageValidator = PackageValidator(this)
        val repeatAction = RepeatModeActionProvider(this, player)

        playbackController.restoreStateFromPreferences(player, session)

        // Configure MediaSessionConnector with player and session
        MediaSessionController(session, playbackController, true).apply {
            setPlayer(player, queueManager, repeatAction)
            setQueueNavigator(queueManager)
            setErrorMessageProvider(errorHandler)
            setMetadataUpdater(queueManager)
        }

        notificationMgr.init()
        session.controller.registerCallback(playbackStateListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service is now started.")

        isStarted = true
        delayedStopHandler.removeCallbacksAndMessages(null)
        delayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "Destroying service.")
        notificationMgr.stopNotification()
        session.controller.unregisterCallback(playbackStateListener)
        isStarted = false

        delayedStopHandler.removeCallbacksAndMessages(null)
        session.release()
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {

        if (!packageValidator.isCallerAllowed(this, clientPackageName, clientUid)) {
            // If the request comes from an untrusted package, return an empty BrowserRoot
            // so that every application can use mediaController in debug mode.
            // Release builds prevents untrusted packages from connecting.
            Log.w(TAG, "onGetRoot: IGNORING request from untrusted package $clientPackageName")
            return if (BuildConfig.DEBUG) BrowserRoot(MediaID.ID_EMPTY_ROOT, null) else null
        }

        return BrowserRoot(MediaID.ID_ROOT, null)
    }

    override fun onLoadChildren(parentId: String, result: MediaItemResult) {
        onLoadChildren(parentId, result, Bundle.EMPTY)
    }

    override fun onLoadChildren(parentId: String, result: MediaItemResult, options: Bundle) {
        Log.v(TAG, "Loading children for ID: $parentId")
        result.detach()
        repository.getMediaItems(parentId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<List<MediaBrowserCompat.MediaItem>> {
                    override fun onSubscribe(d: Disposable) {}

                    override fun onSuccess(items: List<MediaBrowserCompat.MediaItem>) {
                        Log.v(TAG, "Loaded items for $parentId: size=${items.size}")
                        result.sendResult(items)
                    }

                    override fun onError(e: Throwable) {
                        if (e !is UnsupportedOperationException && BuildConfig.DEBUG) {
                            // Rethrow unexpected errors in debug builds
                            throw e
                        }

                        Log.w(TAG, "Unsupported parent id: $parentId")
                        result.sendResult(null)
                    }
                })
    }

    internal fun onPlaybackStart() {
        session.isActive = true
        delayedStopHandler.removeCallbacksAndMessages(null)

        // The service must continue running even after the bound client (usually a MediaController)
        // disconnects, otherwise the music playback will stop.
        // Calling startService(Intent) will keep the service running until it is explicitly killed.

        notificationMgr.startNotification()

        if (!isStarted) {
            Log.i(TAG, "Starting service to keep it running while playing")
            startForegroundService(this, Intent(applicationContext, MusicService::class.java))
        }
    }

    internal fun onPlaybackPaused() {
        stopForeground(false)
    }

    internal fun onPlaybackStop() {
        session.isActive = false
        // Reset the delayed stop handler, so after STOP_DELAY it will be executed again,
        // potentially stopping the service.
        delayedStopHandler.removeCallbacksAndMessages(null)
        delayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY)

        Log.i(TAG, "FOREGROUND: false")
        stopForeground(false)
        notificationMgr.stopNotification()
    }

    override fun notifyChildrenChanged(parentId: String) {
        repository.clear()
        super.notifyChildrenChanged(parentId)
    }

    private inner class PlaybackStateListener : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            when (state.state) {
                PlaybackStateCompat.STATE_PLAYING -> onPlaybackStart()
                PlaybackStateCompat.STATE_PAUSED -> onPlaybackPaused()
                PlaybackStateCompat.STATE_STOPPED -> onPlaybackStop()
            }
        }
    }

    /**
     * Automatically stops the music service if it is inactive for [STOP_DELAY] milliseconds.
     * This allow the service to be stopped when no client is bound to it.
     */
    private class DelayedStopHandler(service: MusicService) : Handler() {
        private val mServiceRef = WeakReference(service)

        override fun handleMessage(msg: Message?) {
            mServiceRef.doIfPresent { service ->
                if (service.player.playWhenReady) {
                    Log.d(TAG, "Ignoring delayed stop since the media player is in use.")
                    return
                }

                Log.i(TAG, "Stopping service with delay handler")
                service.stopSelf()
            }
        }
    }
}
