/*
 * Copyright 2020 Thibault Seisel
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

import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.*
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import fr.nihilus.music.core.context.AppDispatchers
import fr.nihilus.music.core.media.MalformedMediaIdException
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaItems
import fr.nihilus.music.core.media.toMediaId
import fr.nihilus.music.core.os.PermissionDeniedException
import fr.nihilus.music.core.playback.RepeatMode
import fr.nihilus.music.core.settings.Settings
import fr.nihilus.music.media.usage.UsageManager
import fr.nihilus.music.service.browser.BrowserTree
import fr.nihilus.music.service.browser.PaginationOptions
import fr.nihilus.music.service.browser.SearchQuery
import fr.nihilus.music.service.notification.MediaNotificationBuilder
import fr.nihilus.music.service.notification.NOW_PLAYING_NOTIFICATION
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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
    @Inject internal lateinit var subscriptions: SubscriptionManager
    @Inject internal lateinit var notificationBuilder: MediaNotificationBuilder
    @Inject internal lateinit var usageManager: UsageManager

    @Inject internal lateinit var session: MediaSessionCompat
    @Inject internal lateinit var connector: MediaSessionConnector
    @Inject internal lateinit var player: Player
    @Inject internal lateinit var settings: Settings

    private lateinit var mediaController: MediaControllerCompat
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var packageValidator: PackageValidator

    private val controllerCallback = MediaControllerCallback()

    override fun onCreate() {
        super.onCreate()

        // Restore shuffle and repeat mode for the media session and the player (through callbacks)
        mediaController = MediaControllerCompat(this, session)
        mediaController.transportControls.run {
            setShuffleMode(
                when (settings.shuffleModeEnabled) {
                    true -> PlaybackStateCompat.SHUFFLE_MODE_ALL
                    else -> PlaybackStateCompat.SHUFFLE_MODE_NONE
                }
            )
            setRepeatMode(
                when (settings.repeatMode) {
                    RepeatMode.ALL -> PlaybackStateCompat.REPEAT_MODE_ALL
                    RepeatMode.ONE -> PlaybackStateCompat.REPEAT_MODE_ONE
                    else -> PlaybackStateCompat.REPEAT_MODE_NONE
                }
            )
        }

        // Because ExoPlayer will manage the MediaSession, add the service as a callback for state changes.
        mediaController.registerCallback(controllerCallback)

        subscriptions.updatedParentIds
            .onEach { updatedParentId -> notifyChildrenChanged(updatedParentId.toString()) }
            .launchIn(this)

        // Listen to track completion events
        val completionListener = TrackCompletionListener()
        player.addListener(completionListener)

        notificationManager = NotificationManagerCompat.from(this)
        packageValidator = PackageValidator(this, R.xml.svc_allowed_media_browser_callers)

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

            BrowserRoot(MediaId.ROOT, Bundle(3).apply {
                putBoolean(AutomotiveExtras.MEDIA_SEARCH_SUPPORTED, true)
                putBoolean(AutomotiveExtras.CONTENT_STYLE_SUPPORTED, true)
                putInt(AutomotiveExtras.CONTENT_STYLE_BROWSABLE_HINT, AutomotiveExtras.CONTENT_STYLE_GRID_ITEM_HINT_VALUE)
                putInt(AutomotiveExtras.CONTENT_STYLE_PLAYABLE_HINT, AutomotiveExtras.CONTENT_STYLE_LIST_ITEM_HINT_VALUE)
            })
        } else null
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaItem>>
    ): Unit = onLoadChildren(parentId, result, Bundle.EMPTY)

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaItem>>,
        options: Bundle
    ) {
        result.detach()

        launch {
            try {
                val parentMediaId = parentId.toMediaId()
                val paginationOptions = getPaginationOptions(options)
                val children = subscriptions.loadChildren(parentMediaId, paginationOptions)

                val builder = MediaDescriptionCompat.Builder()
                result.sendResult(children.map { it.toItem(builder) })

            } catch (malformedId: MalformedMediaIdException) {
                Timber.i(malformedId, "Unable to load children of %s: malformed media id", parentId)
                result.sendResult(null)

            } catch (pde: PermissionDeniedException) {
                Timber.i("Unable to load children of %s: denied permission %s", parentId, pde.permission)
                result.sendResult(null)

            } catch (invalidParent: NoSuchElementException) {
                Timber.i("Unable to load children of %s: not a browsable item from the tree", parentId)
                result.sendResult(null)
            }
        }
    }

    private fun getPaginationOptions(options: Bundle): PaginationOptions? {
        return options.takeIf {
            it.containsKey(EXTRA_PAGE) || it.containsKey(EXTRA_PAGE_SIZE)
        }?.let {
            PaginationOptions(
                it.getInt(EXTRA_PAGE, PaginationOptions.DEFAULT_PAGE_NUMBER),
                it.getInt(EXTRA_PAGE_SIZE, PaginationOptions.DEFAULT_PAGE_SIZE)
            )
        }
    }

    override fun onLoadItem(itemId: String?, result: Result<MediaItem>) {
        if (itemId == null) {
            result.sendResult(null)
        } else {
            result.detach()
            launch {
                try {
                    val itemMediaId = itemId.toMediaId()
                    val requestedContent = subscriptions.getItem(itemMediaId)
                    result.sendResult(requestedContent?.toItem())

                } catch (malformedId: MalformedMediaIdException) {
                    Timber.i(malformedId, "Attempt to load item from a malformed media id: %s", itemId)
                    result.sendResult(null)

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
        result: Result<List<MediaItem>>
    ) {
        result.detach()
        launch(dispatchers.Default) {
            val parsedQuery = SearchQuery.from(query, extras)

            try {
                val searchResults = browserTree.search(parsedQuery)
                val builder = MediaDescriptionCompat.Builder()
                result.sendResult(searchResults.map { it.toItem(builder) })

            } catch (pde: PermissionDeniedException) {
                Timber.i("Unable to search %s due to missing permission: %s", query, pde.permission)
                result.sendResult(null)
            }
        }
    }

    private fun MediaContent.toItem(
        builder: MediaDescriptionCompat.Builder = MediaDescriptionCompat.Builder()
    ): MediaItem {
        builder
            .setMediaId(id.encoded)
            .setTitle(title)
            .setIconUri(iconUri)

        when (this) {
            is MediaCategory -> {
                builder
                    .setSubtitle(subtitle)
                    .setExtras(Bundle().apply {
                        putInt(MediaItems.EXTRA_NUMBER_OF_TRACKS, count)
                    })
            }

            is AudioTrack -> {
                builder
                    .setSubtitle(artist)
                    .setExtras(Bundle(3).apply {
                        putInt(MediaItems.EXTRA_DISC_NUMBER, disc)
                        putInt(MediaItems.EXTRA_TRACK_NUMBER, number)
                        putLong(MediaItems.EXTRA_DURATION, duration)
                    })
            }
        }

        var flags = 0
        if (browsable) {
            flags = flags or MediaItem.FLAG_BROWSABLE
        }
        if (playable) {
            flags = flags or MediaItem.FLAG_PLAYABLE
        }

        return MediaItem(builder.build(), flags)
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
            settings.shuffleModeEnabled = shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            settings.repeatMode = when (repeatMode) {
                PlaybackStateCompat.REPEAT_MODE_ALL,
                PlaybackStateCompat.REPEAT_MODE_GROUP -> RepeatMode.ALL
                PlaybackStateCompat.REPEAT_MODE_ONE -> RepeatMode.ONE
                else -> RepeatMode.DISABLED
            }
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

                else -> {
                    // Intentionally empty.
                }
            }
        }

        private fun onPlaybackStarted() {
            // Activate the media session if not active
            if (!session.isActive) {
                session.isActive = true
            }

            // Display a notification, putting the service to the foreground.
            val notification = notificationBuilder.buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOW_PLAYING_NOTIFICATION,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOW_PLAYING_NOTIFICATION, notification)
            }

            // Start the service to keep it playing even when all clients unbound.
            this@MusicService.startSelf()
        }

        private fun onPlaybackPaused() {
            // Put the service back to the background, keeping the notification
            stopForeground(false)

            // Update the notification content if the session is active
            if (session.isActive) {
                notificationManager.notify(NOW_PLAYING_NOTIFICATION, notificationBuilder.buildNotification())
            }
        }

        private fun onPlaybackStopped() {
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

            player.currentTimeline.getWindow(completedTrackIndex, windowBuffer)

            val completedMedia = windowBuffer.tag as AudioTrack
            val completedTrackId = checkNotNull(completedMedia.id.track) {
                "Track ${completedMedia.title} has an invalid media id: ${completedMedia.id}"
            }

            launch {
                usageManager.reportCompletion(completedTrackId)
            }
        }
    }
}