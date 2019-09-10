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

import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import fr.nihilus.music.media.*
import fr.nihilus.music.media.actions.ActionFailure
import fr.nihilus.music.media.actions.BrowserAction
import fr.nihilus.music.media.actions.CustomActions
import fr.nihilus.music.media.permissions.PermissionDeniedException
import fr.nihilus.music.media.tree.BrowserTree
import fr.nihilus.music.media.tree.SearchQuery
import fr.nihilus.music.media.usage.UsageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.collect
import timber.log.Timber
import javax.inject.Inject

class MusicService : BaseBrowserService() {

    companion object {
        /**
         * The name of an [intent action][Intent.getAction] for launching the activity
         * associated with the media session and display the UI for controlling media playback.
         */
        const val ACTION_PLAYER_UI = "fr.nihilus.music.media.action.PLAYER_UI"
    }

    @Inject internal lateinit var dispatchers: AppDispatchers
    @Inject internal lateinit var browserTree: BrowserTree
    @Inject internal lateinit var notificationBuilder: MediaNotificationBuilder
    @Inject internal lateinit var usageManager: UsageManager

    @Inject internal lateinit var session: MediaSessionCompat
    @Inject internal lateinit var connector: MediaSessionConnector
    @Inject internal lateinit var player: Player
    @Inject internal lateinit var settings: MediaSettings
    @Inject internal lateinit var customActions: Map<String, @JvmSuppressWildcards BrowserAction>

    private lateinit var mediaController: MediaControllerCompat
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var becomingNoisyReceiver: BecomingNoisyReceiver
    private lateinit var packageValidator: PackageValidator

    private val controllerCallback = MediaControllerCallback()

    override fun onCreate() {
        super.onCreate()

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
        packageValidator = PackageValidator(this, R.xml.svc_allowed_media_browser_callers)

        // Observe changes to the "skip silence" settings, applying it to the player when changed.
        observeSkipSilenceSettings()

        // Listen for changes in the repository to notify media browsers.
        // If the changed media ID is a track, notify for its parent category.
        observeMediaChanges()

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
    }

    override fun onDestroy() {
        Timber.i("Destroying service.")
        session.release()
        super.onDestroy()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        // Check the caller's signature and disconnect it if not allowed by returning `null`.
        return if (packageValidator.isKnownCaller(clientPackageName, clientUid)) {
            // Grant permission to known callers to read album arts without storage permissions.
            grantUriPermission(
                clientPackageName,
                Uri.parse("content://${BuildConfig.APP_PROVIDER_AUTHORITY}/"),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
            BrowserRoot(MediaId.ROOT, null)
        } else null
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ): Unit = onLoadChildren(parentId, result, Bundle.EMPTY)

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>,
        options: Bundle
    ) {
        result.detach()

        launch(dispatchers.Default) {
            val parentMediaId = parentId.toMediaIdOrNull()
            if (parentMediaId != null) {
                try {
                    val children = browserTree.getChildren(parentMediaId, options)
                    result.sendResult(children)

                } catch (pde: PermissionDeniedException) {
                    Timber.i("Loading children of %s failed due to missing permission: %s", parentId, pde.permission)
                    result.sendResult(null)
                }

            } else {
                Timber.i("Attempt to load children of an invalid media id: %s", parentId)
                result.sendResult(null)
            }
        }
    }

    override fun onLoadItem(itemId: String?, result: Result<MediaBrowserCompat.MediaItem>) {
        if (itemId == null) {
            result.sendResult(null)
        } else {
            val itemMediaId = itemId.toMediaIdOrNull() ?: run {
                Timber.i("Attempt to load item with an invalid media id: %s", itemId)
                result.sendResult(null)
                return
            }

            result.detach()
            launch(dispatchers.Default) {
                try {
                    val item = browserTree.getItem(itemMediaId)
                    result.sendResult(item)
                } catch (pde: PermissionDeniedException) {
                    Timber.i("Loading item %s failed due to missing permission: %s", itemId, pde.permission)
                    result.sendResult(null)
                }
            }
        }
    }

    override fun onSearch(
        query: String,
        extras: Bundle?,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        Log.i("AssistantSearch", "Searching $query. Extras=$extras")
        result.detach()
        launch(dispatchers.Default) {
            val parsedQuery = SearchQuery.from(query, extras)
            val searchResults = browserTree.search(parsedQuery)
            result.sendResult(searchResults)
        }
    }

    override fun onCustomAction(action: String, extras: Bundle?, result: Result<Bundle>) {
        val customAction = customActions[action] ?: run {
            Timber.w("Attempt to execute an unsupported custom action: %s", action)
            result.sendError(null)
            return
        }

        result.detach()
        launch(dispatchers.Default) {
            try {
                val resultBundle = customAction.execute(extras)
                result.sendResult(resultBundle)
            } catch (failure: ActionFailure) {
                result.sendError(Bundle(2).apply {
                    putInt(CustomActions.EXTRA_ERROR_CODE, failure.errorCode)
                    putString(CustomActions.EXTRA_ERROR_MESSAGE, failure.errorMessage)
                })
            }
        }
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
            mediaController.playbackState?.let(this::updateServiceState)
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            state?.let(this::updateServiceState)
        }

        override fun onShuffleModeChanged(shuffleMode: Int) {
            settings.shuffleMode = shuffleMode
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            settings.repeatMode = repeatMode
        }

        private fun updateServiceState(state: PlaybackStateCompat) {
            val updatedState = state.state
            if (mediaController.metadata == null) {
                // Do not update service when no metadata.
                return
            }

            when (updatedState) {
                // Playback started or has been resumed.
                PlaybackStateCompat.STATE_PLAYING -> onPlaybackStarted()

                // Playback has been paused.
                PlaybackStateCompat.STATE_PAUSED -> onPlaybackPaused()

                // Playback ended or an error occurred.
                PlaybackStateCompat.STATE_NONE,
                PlaybackStateCompat.STATE_STOPPED,
                PlaybackStateCompat.STATE_ERROR -> onPlaybackStopped()
            }
        }

        private fun onPlaybackStarted() {
            // Activate the media session if not active
            if (!session.isActive) {
                session.isActive = true
            }

            // Start listening for audio becoming noisy events
            becomingNoisyReceiver.register()

            // Display a notification, putting the service to the foreground.
            val notification = notificationBuilder.buildNotification()
            startForeground(NOW_PLAYING_NOTIFICATION, notification)

            // Start the service to keep it playing even when all clients unbound.
            this@MusicService.startSelf()
        }

        private fun onPlaybackPaused() {
            // Stop listening for audio becoming noisy events since playback is already paused.
            becomingNoisyReceiver.unregister()

            // Put the service back to the background, keeping the notification
            stopForeground(false)

            // Update the notification content if the session is active
            if (session.isActive) {
                notificationManager.notify(NOW_PLAYING_NOTIFICATION, notificationBuilder.buildNotification())
            }
        }

        private fun onPlaybackStopped() {
            // We should not receive "audio becoming noisy" events at this point.
            becomingNoisyReceiver.unregister()

            // Clear notification and service foreground status
            stopForeground(true)

            // De-activate the media session.
            if (session.isActive) {
                session.isActive = false
            }

            // Stop the service, killing it if it is not bound.
            this@MusicService.stop()
        }

        override fun onSessionDestroyed() {
            player.run {
                stop()
                release()
            }
        }
    }

    private fun CoroutineScope.observeSkipSilenceSettings() = launch {
        settings.skipSilenceUpdates.consumeEach { shouldSkipSilence ->
            player.playbackParameters = if (shouldSkipSilence) {
                PlaybackParameters(1f, 1f, true)
            } else PlaybackParameters.DEFAULT
        }
    }

    private fun CoroutineScope.observeMediaChanges() = launch {
        browserTree.updatedParentIds.collect { parentId ->
            notifyChildrenChanged(parentId.encoded)
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
            val completedMedia = windowBuffer.tag as? MediaDescriptionCompat ?: return
            val (_, _, completedTrackId) = completedMedia.mediaId.toMediaId()

            if (completedTrackId != null) {
                usageManager.reportCompletion(completedTrackId)
            }
        }
    }
}