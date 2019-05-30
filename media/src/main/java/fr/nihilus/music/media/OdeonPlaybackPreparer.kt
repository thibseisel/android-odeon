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

package fr.nihilus.music.media

import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.ShuffleOrder
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import fr.nihilus.music.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.media.MediaId.Builder.encode
import fr.nihilus.music.media.permissions.PermissionDeniedException
import fr.nihilus.music.media.playback.AudioOnlyExtractorsFactory
import fr.nihilus.music.media.service.MusicService
import fr.nihilus.music.media.tree.BrowserTree
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Handle requests to prepare media that can be played from the Odeon Media Player.
 * This fetches media information from the music library.
 */
internal class OdeonPlaybackPreparer
@Inject constructor(
    private val service: MusicService,
    private val player: ExoPlayer,
    private val browserTree: BrowserTree,
    private val settings: MediaSettings
) : MediaSessionConnector.PlaybackPreparer {

    private val audioOnlyExtractors = AudioOnlyExtractorsFactory()
    private val appDataSourceFactory = DefaultDataSourceFactory(
        service,
        Util.getUserAgent(service, service.getString(R.string.app_name))
    )

    override fun getSupportedPrepareActions(): Long {
        return PlaybackStateCompat.ACTION_PREPARE or
                PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH or
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
    }

    /**
     * Handles generic requests to prepare playback.
     *
     * This prepares the last played queue, in the same order as the last time it was played.
     * If it is the first time a queue is built, this prepares to play all tracks.
     *
     * @see MediaSessionCompat.Callback.onPrepare
     */
    override fun onPrepare() {
        // Should prepare playing the "current" media, which is the last played media id.
        // If not available, play all songs.
        val lastPlayedMediaId = settings.lastPlayedMediaId ?: encode(TYPE_TRACKS, CATEGORY_ALL)
        prepareFromMediaId(lastPlayedMediaId)
    }

    /**
     * Handles requests to prepare for playing a specific mediaId.
     *
     * This will built a new queue even if the current media id is the same, shuffling tracks
     * in a different order.
     * If no media id is provided, the player will be prepared to play all tracks.
     *
     * @param mediaId The media id of the track of set of tracks to prepare.
     * @param extras Optional parameters describing how the queue should be prepared.
     *
     * @see MediaSessionCompat.Callback.onPrepareFromMediaId
     */
    override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
        // A new queue has been requested. Increment the queue identifier.
        settings.queueCounter++
        prepareFromMediaId(mediaId ?: encode(TYPE_TRACKS, CATEGORY_ALL))
    }

    override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) {
        // Not supported at the time.
        throw UnsupportedOperationException()
    }

    override fun onPrepareFromSearch(query: String?, extras: Bundle?) {
        if (query.isNullOrEmpty()) {
            // Generic query, such as "play music"
            onPrepare()

        } else service.launch {
            val results = browserTree.search(query, extras)

            val firstResult = results.firstOrNull()
            val playQueue = if (firstResult?.isBrowsable == true) {
                    browserTree.getChildren(firstResult.mediaId.toMediaId(), null).orEmpty()
            } else results

            preparePlayer(playQueue.filter { it.isPlayable && !it.isBrowsable }, 0)
        }
    }

    override fun getCommands(): Array<String>? = null

    override fun onCommand(
        player: Player?,
        command: String?,
        extras: Bundle?,
        cb: ResultReceiver?
    ) = Unit

    private fun prepareFromMediaId(mediaId: String) = service.launch {
        try {
            val (type, category, track) = mediaId.toMediaId()
            val parentId = MediaId.fromParts(type, category, track = null)

            val children = browserTree.getChildren(parentId, null)

            if (children != null) {
                val playQueue = children.filter { it.isPlayable && !it.isBrowsable }
                val startIndex = if (track == null) -1 else playQueue.indexOfFirst { it.mediaId == mediaId }
                preparePlayer(playQueue, startIndex)

            } else {
                Timber.i("Unable to prepare playback: %s is not part of the hierarchy", mediaId)
            }

        } catch (pde: PermissionDeniedException) {
            Timber.i("Unable to prepare from media id due to missing permission: %s", pde.permission)

        } catch (ime: InvalidMediaException) {
            Timber.i("Attempt to prepare playback from an invalid media id: %s", mediaId)
        }
    }

    private fun preparePlayer(playQueue: List<MediaBrowserCompat.MediaItem>, startIndex: Int) {
        // Short-circuit: if there are no playable items.
        if (playQueue.isEmpty()) {
            return
        }

        val mediaSources = Array(playQueue.size) {
            val playableItem: MediaDescriptionCompat = playQueue[it].description
            val sourceUri = checkNotNull(playableItem.mediaUri) {
                "Track ${playableItem.mediaId} (${playableItem.title} should have a media Uri."
            }

            ExtractorMediaSource.Factory(appDataSourceFactory)
                .setExtractorsFactory(audioOnlyExtractors)
                .setTag(playableItem)
                .createMediaSource(sourceUri)
        }

        // Defines a shuffle order for the loaded media sources that is predictable.
        // It depends on the number of time a new queue has been built.
        val predictableShuffleOrder = ShuffleOrder.DefaultShuffleOrder(
            mediaSources.size,
            settings.queueCounter
        )

        // Concatenate all media source to play them all in the same Timeline.
        val concatenatedSource = ConcatenatingMediaSource(
            false,
            predictableShuffleOrder,
            *mediaSources
        )

        player.prepare(concatenatedSource)

        // Start playback at a given track if specified.
        if (startIndex >= 0) {
            player.seekTo(startIndex.coerceAtMost(mediaSources.lastIndex), C.TIME_UNSET)
        }
    }
}