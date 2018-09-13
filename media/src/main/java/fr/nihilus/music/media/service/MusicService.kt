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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.content.ContextCompat.startForegroundService
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import dagger.android.AndroidInjection
import fr.nihilus.music.media.*
import fr.nihilus.music.media.repo.MusicRepository
import fr.nihilus.music.media.utils.MediaID
import fr.nihilus.music.media.utils.PermissionDeniedException
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class MusicService : MediaBrowserServiceCompat() {

    @Inject internal lateinit var repository: MusicRepository
    @Inject internal lateinit var notificationMgr: MediaNotificationManager

    @Inject internal lateinit var session: MediaSessionCompat
    @Inject internal lateinit var connector: MediaSessionConnector
    @Inject internal lateinit var settings: MediaSettings

    private lateinit var becomingNoisyReceiver: BecomingNoisyReceiver
    private lateinit var packageValidator: PackageValidator

    private val controllerCallback = MediaControllerCallback()
    private val subscriptions = CompositeDisposable()

    private var isStarted = false

    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)

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

        // Restore last configured shuffle mode and repeat mode settings.
        // Player will be configured accordingly through callbacks.
        session.setShuffleMode(settings.shuffleMode)
        session.setRepeatMode(settings.repeatMode)

        session.controller.registerCallback(controllerCallback)

        becomingNoisyReceiver = BecomingNoisyReceiver(this, session.sessionToken)
        packageValidator = PackageValidator(this, R.xml.abc_allowed_media_browser_callers)

        // Listen for changes in the repository to notify media browsers.
        // If the changed media ID is a track, notify for its parent category.
        repository.getMediaChanges()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { browseCategoryOf(it) }
            .subscribe(this::notifyChildrenChanged)
            .also { subscriptions.add(it) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.i("Service is now started.")
        MediaButtonReceiver.handleIntent(session, intent)

        isStarted = true
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Timber.i("Destroying service.")
        notificationMgr.stop(clearNotification = true)
        session.controller.unregisterCallback(controllerCallback)
        isStarted = false

        session.run {
            isActive = false
            release()
        }

        // Clear all subscriptions to prevent resource leaks
        subscriptions.clear()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        if (!packageValidator.isCallerAllowed(clientPackageName, clientUid)) {
            // If the request comes from an untrusted package, return an empty BrowserRoot
            // so that every application can use mediaController in debug mode.
            // Release builds prevents untrusted packages from connecting.
            Timber.w("onGetRoot: IGNORING request from untrusted package %s", clientPackageName)
            return if (BuildConfig.DEBUG) BrowserRoot(MediaID.ID_EMPTY_ROOT, null) else null
        }

        return BrowserRoot(BROWSER_ROOT, null)
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        onLoadChildren(parentId, result, Bundle.EMPTY)
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>, options: Bundle) {
        Timber.v("Start loading children for ID: %s", parentId)
        result.detach()
        repository.getMediaItems(parentId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<List<MediaBrowserCompat.MediaItem>> {
                override fun onSubscribe(d: Disposable) {}

                override fun onSuccess(items: List<MediaBrowserCompat.MediaItem>) {
                    Timber.v("Loaded items for %s: size=%d", parentId, items.size)
                    result.sendResult(items)
                }

                override fun onError(e: Throwable) {
                    when (e) {
                        is UnsupportedOperationException -> Timber.w("Unsupported parent id: %s", parentId)
                        is PermissionDeniedException -> Timber.i(e)
                        else -> Timber.e(e, "Unexpected error while loading %s children", parentId)
                    }

                    result.sendResult(null)
                }
            })
    }

    internal fun onPlaybackStart() {
        // The service must continue running even after the bound client (usually a MediaController)
        // disconnects, otherwise the music playback will stop.
        // Calling startService(Intent) will keep the service running until it is explicitly killed.

        notificationMgr.start()

        if (!isStarted) {
            Timber.i("Starting service to keep it running while playing")
            startForegroundService(this, Intent(applicationContext, MusicService::class.java))
        }
    }

    internal fun onPlaybackPaused() {
        Timber.v("onPlaybackPause")
        notificationMgr.stop(clearNotification = false)
    }

    internal fun onPlaybackStop() {
        Timber.v("onPlaybackStop")
        session.isActive = false
        notificationMgr.stop(clearNotification = true)
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat) = when (state.state) {
            PlaybackStateCompat.STATE_PLAYING -> {
                becomingNoisyReceiver.register()
                onPlaybackStart()
            }
            PlaybackStateCompat.STATE_PAUSED -> {
                becomingNoisyReceiver.unregister()
                onPlaybackPaused()
            }
            PlaybackStateCompat.STATE_STOPPED,
            PlaybackStateCompat.STATE_NONE -> {
                becomingNoisyReceiver.unregister()
                onPlaybackStop()
            }
            else -> becomingNoisyReceiver.unregister()
        }

        override fun onShuffleModeChanged(shuffleMode: Int) {
            settings.shuffleMode = shuffleMode
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            settings.repeatMode = repeatMode
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
}
