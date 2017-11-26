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
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import dagger.android.AndroidInjection
import fr.nihilus.music.BuildConfig
import fr.nihilus.music.HomeActivity
import fr.nihilus.music.MediaItemsResult
import fr.nihilus.music.media.repo.MusicRepository
import fr.nihilus.music.playback.PlaybackManager
import fr.nihilus.music.playback.QueueManager
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

class MusicService : MediaBrowserServiceCompat(),
        PlaybackManager.ServiceCallback, QueueManager.MetadataUpdateListener {

    @Inject internal lateinit var repository: MusicRepository
    @Inject internal lateinit var playbackMgr: PlaybackManager
    @Inject internal lateinit var notificationMgr: MediaNotificationManager

    private lateinit var mSession: MediaSessionCompat
    private val mDelayedStopHandler = DelayedStopHandler(this)

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()

        mSession = MediaSessionCompat(this, TAG)
        sessionToken = mSession.sessionToken
        mSession.setCallback(playbackMgr.mediaSessionCallback)
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

        val appContext = applicationContext
        val uiIntent = Intent(appContext, HomeActivity::class.java)
        val pi = PendingIntent.getActivity(appContext, 99,
                uiIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        mSession.setSessionActivity(pi)
        mSession.setRatingType(RatingCompat.RATING_NONE)

        playbackMgr.init()
        playbackMgr.updatePlaybackState(null)

        notificationMgr.init()

        Log.i(TAG, "Service fully initialized")
        mSession.isActive = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            if (ACTION_CMD == intent.action) {
                val cmd = intent.getStringExtra(CMD_NAME)
                if (CMD_PAUSE == cmd) {
                    playbackMgr.handlePauseRequest()
                }
            }
        }

        Log.i(TAG, "Service is now started.")

        mDelayedStopHandler.removeCallbacksAndMessages(null)
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "Destroying service.")
        playbackMgr.handleStopRequest(null)
        notificationMgr.stopNotification()

        mDelayedStopHandler.removeCallbacksAndMessages(null)
        mSession.release()
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        val thisApplicationPackage = application.packageName
        if (clientPackageName != thisApplicationPackage) {
            // The media session this app holds is private.
            // We deny access to any app trying to connect to it.
            Log.w(TAG, "onGetRoot: IGNORING request from untrusted package $clientPackageName")
            return null
        }

        return BrowserRoot(MediaID.ID_ROOT, null)
    }

    override fun onLoadChildren(parentId: String, result: MediaItemsResult) {
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

    override fun onPlaybackStart() {
        mSession.isActive = true
        mDelayedStopHandler.removeCallbacksAndMessages(null)

        // The service must continue running even after the bound client (usually a MediaController)
        // disconnects, otherwise the music playback will stop.
        // Calling startService(Intent) will keep the service running until it is explicitly killed.

        Log.i(TAG, "Starting service to keep it running while playing")
        startForegroundService(this, Intent(applicationContext, MusicService::class.java))
    }

    override fun onPlaybackStop() {
        mSession.isActive = false
        // Reset the delayed stop handler, so after STOP_DELAY it will be executed again,
        // potentially stopping the service.
        mDelayedStopHandler.removeCallbacksAndMessages(null)
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY)

        Log.i(TAG, "FOREGROUND: false")
        stopForeground(false)
    }

    override fun onShuffleModeChanged(@PlaybackStateCompat.ShuffleMode shuffleMode: Int) {
        mSession.setShuffleMode(shuffleMode)
    }

    override fun onNotificationRequired() {
        Log.i(TAG, "Require a notification")
        notificationMgr.startNotification()
    }

    override fun onPlaybackStateUpdated(newState: PlaybackStateCompat) {
        mSession.setPlaybackState(newState)
    }

    override fun onMetadataChanged(metadata: MediaMetadataCompat) {
        mSession.setMetadata(metadata)
    }

    override fun onMetadataRetrieveError() {
        Log.e(TAG, "MetadataRetrieveError")
        playbackMgr.updatePlaybackState("No metadata")
    }

    override fun onCurrentQueueIndexUpdated(queueIndex: Int) {
        playbackMgr.handlePlayRequest()
    }

    override fun onQueueUpdated(title: String, newQueue: List<MediaSessionCompat.QueueItem>) {
        mSession.setQueueTitle(title)
        mSession.setQueue(newQueue)
    }

    override fun notifyChildrenChanged(parentId: String) {
        repository.clear()
        super.notifyChildrenChanged(parentId)
    }

    companion object {
        const val ACTION_CMD = "fr.nihilus.music.ACTION_CMD"
        const val CMD_NAME = "CMD_NAME"
        const val CMD_PAUSE = "CMD_PAUSE"
    }

    /**
     * Automatically stops the music service if it is inactive for [STOP_DELAY] milliseconds.
     * This allow the service to be stopped when no client is bound to it.
     */
    private class DelayedStopHandler(service: MusicService) : Handler() {
        private val mServiceRef = WeakReference(service)

        override fun handleMessage(msg: Message?) {
            mServiceRef.get()?.let {
                if (it.playbackMgr.musicPlayer.isPlaying) {
                    Log.d(TAG, "Ignoring delayed stop since the media player is in use.")
                    return
                }

                Log.i(TAG, "Stopping service with delay handler")
                it.stopSelf()
            }
        }
    }
}
