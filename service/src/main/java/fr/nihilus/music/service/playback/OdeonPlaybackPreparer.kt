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

package fr.nihilus.music.service.playback

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ShuffleOrder
import fr.nihilus.music.core.context.AppDispatchers
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.core.media.parse
import fr.nihilus.music.core.os.PermissionDeniedException
import fr.nihilus.music.core.settings.Settings
import fr.nihilus.music.service.AudioTrack
import fr.nihilus.music.service.MediaCategory
import fr.nihilus.music.service.MediaSessionConnector
import fr.nihilus.music.service.ServiceCoroutineScope
import fr.nihilus.music.service.browser.BrowserTree
import fr.nihilus.music.service.browser.SearchQuery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.random.Random

/**
 * Handle requests to prepare media that can be played from the Odeon Media Player.
 * This fetches media information from the music library.
 */
internal class OdeonPlaybackPreparer @Inject constructor(
    @ServiceCoroutineScope private val scope: CoroutineScope,
    private val dispatchers: AppDispatchers,
    private val player: ExoPlayer,
    private val browserTree: BrowserTree,
    private val settings: Settings
) : MediaSessionConnector.PlaybackPreparer {

    override fun getSupportedPrepareActions(): Long =
        PlaybackStateCompat.ACTION_PREPARE or
        PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
        PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH or
        PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH

    /**
     * Handles generic requests to prepare playback.
     *
     * This prepares the last played queue, in the same order as the last time it was played.
     * If it is the first time a queue is built, this prepares to play all tracks.
     *
     * @see MediaSessionCompat.Callback.onPrepare
     */
    override fun onPrepare(playWhenReady: Boolean) {
        // Should prepare playing the "current" media, which is the last played media id.
        // If not available, play all songs.
        val reloadStrategy = settings.queueReload
        prepareFromMediaId(
            mediaId = settings.lastQueueMediaId ?: MediaId(TYPE_TRACKS, CATEGORY_ALL),
            startPlaybackPosition = when {
                reloadStrategy.reloadTrack -> settings.lastQueueIndex
                else -> 0
            },
            playbackPosition = when {
                reloadStrategy.reloadPosition -> settings.lastPlayedPosition
                else -> C.TIME_UNSET
            },
            playWhenReady = playWhenReady
        )
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
    override fun onPrepareFromMediaId(mediaId: String?, playWhenReady: Boolean, extras: Bundle?) {
        // A new queue has been requested. Update the last played queue media id (the queue identifier will change).
        val queueMediaId = mediaId?.parse()
            ?: MediaId(TYPE_TRACKS, CATEGORY_ALL)
        settings.lastQueueMediaId = queueMediaId

        prepareFromMediaId(queueMediaId, C.POSITION_UNSET, C.TIME_UNSET, playWhenReady)
    }

    override fun onPrepareFromUri(uri: Uri?, playWhenReady: Boolean, extras: Bundle?) {
        // Not supported at the time.
        throw UnsupportedOperationException()
    }

    /**
     * Handle requests to prepare for playing tracks picked from the results of a search.
     */
    @SuppressLint("LogNotTimber")
    override fun onPrepareFromSearch(query: String?, playWhenReady: Boolean, extras: Bundle?) {
        // TODO Remove those lines when got enough info on how Assistant understands voice searches.
        if (Log.isLoggable("AssistantSearch", Log.INFO) && extras != null) {
            val extString = extras.keySet()
                .joinToString(", ", "{", "}") { "$it=${extras[it]}" }
            Log.i("AssistantSearch", "onPrepareFromSearch: query=\"$query\", extras=$extString")
        }

        val parsedQuery = SearchQuery.from(query, extras)
        if (parsedQuery is SearchQuery.Empty) {
            // Generic query, such as "play music"
            onPrepare(playWhenReady)

        } else scope.launch(dispatchers.Default) {
            val results = browserTree.search(parsedQuery)

            val firstResult = results.firstOrNull()
            if (firstResult is MediaCategory) {
                prepareFromMediaId(firstResult.id, C.POSITION_UNSET, C.TIME_UNSET, playWhenReady)
            } else {
                preparePlayer(
                    results.filterIsInstance<AudioTrack>(),
                    firstShuffledIndex = 0,
                    startIndex = 0,
                    playbackPosition = C.TIME_UNSET,
                    playWhenReady = playWhenReady
                )
            }
        }
    }

    private suspend fun loadPlayableChildrenOf(parentId: MediaId): List<AudioTrack> = try {
        val children = browserTree.getChildren(parentId).first()
        children.filterIsInstance<AudioTrack>()

    } catch (pde: PermissionDeniedException) {
        Timber.i("Unable to load children of %s: denied permission %s", parentId, pde.permission)
        emptyList()

    } catch (invalidParent: NoSuchElementException) {
        Timber.i("Unable to load children of %s: not a browsable item from the tree", parentId)
        emptyList()
    }

    private fun prepareFromMediaId(
        mediaId: MediaId,
        startPlaybackPosition: Int,
        playbackPosition: Long,
        playWhenReady: Boolean
    ) = scope.launch(dispatchers.Default) {
        val parentId = mediaId.copy(track = null)

        val playQueue = loadPlayableChildrenOf(parentId)
        val firstIndex = when {
            mediaId.track != null -> playQueue.indexOfFirst { it.id == mediaId }
            else -> C.POSITION_UNSET
        }

        preparePlayer(
            playQueue,
            firstShuffledIndex = firstIndex,
            startIndex = startPlaybackPosition,
            playbackPosition = playbackPosition,
            playWhenReady
        )
    }

    /**
     * Prepare playback of a given [playQueue]
     * and start playing the index at [startIndex] when ready.
     *
     * @param playQueue The items to be played. All media should be playable and have a media uri.
     * @param firstShuffledIndex The index of the item that should be the first when playing shuffled.
     * This should be a valid index in [playQueue], otherwise an index is chosen randomly.
     * @param startIndex The index of the item that should be played when the player is ready.
     * This should be a valid index in [playQueue],
     * otherwise playback will be set to start at the first index in the queue (shuffled or not).
     */
    private suspend fun preparePlayer(
        playQueue: List<AudioTrack>,
        firstShuffledIndex: Int,
        startIndex: Int,
        playbackPosition: Long,
        playWhenReady: Boolean
    ) {
        if (playQueue.isNotEmpty()) withContext(dispatchers.Main) {
            val queueItems = playQueue.map { track ->
                MediaItem.Builder()
                    .setMediaId(track.id.encoded)
                    .setUri(track.mediaUri)
                    .setTag(track)
                    .build()
            }

            // Defines a shuffle order for the loaded media sources that is predictable.
            // The random seed is built from an unique queue identifier,
            // so that queue can be rebuilt with the same order.
            val randomSeed = settings.queueIdentifier

            // Create a shuffle order that starts with the track at the specified "first index".
            // If that index is invalid, just randomly shuffle the play queue.
            val predictableShuffleOrder = if (firstShuffledIndex in playQueue.indices) {
                val shuffledIndices =
                    createShuffledIndices(firstShuffledIndex, playQueue.size, randomSeed)
                ShuffleOrder.DefaultShuffleOrder(shuffledIndices, randomSeed)
            } else {
                ShuffleOrder.DefaultShuffleOrder(playQueue.size, randomSeed)
            }

            // Start playback at a given position if specified, otherwise at first shuffled index.
            val targetPlaybackPosition = when (startIndex) {
                in playQueue.indices -> startIndex
                else -> predictableShuffleOrder.firstIndex
            }

            player.setMediaItems(queueItems, targetPlaybackPosition, playbackPosition)
            player.setShuffleOrder(predictableShuffleOrder)
            player.prepare()

            player.playWhenReady = playWhenReady
        }
    }

    /**
     * Create a sequence of consecutive natural numbers between `0` and `length - 1` in shuffled order,
     * starting by the given [firstIndex].
     *
     * @param firstIndex The first value in the produces array. Must be between `0` and [length] (exclusive).
     * @param length The length of the produced array. Must be greater or equal to `0`.
     * @param randomSeed The seed for shuffling numbers.
     *
     * @return An array containing all the natural numbers between `0` and `length - 1` in shuffled order.
     * Its first element is [firstIndex].
     */
    private fun createShuffledIndices(firstIndex: Int, length: Int, randomSeed: Long): IntArray {
        val shuffled = IntArray(length)

        if (length > 0) {
            val random = Random(randomSeed)
            shuffled[0] = firstIndex

            for (i in 1..firstIndex) {
                val swapIndex = random.nextInt(1, i + 1)
                shuffled[i] = shuffled[swapIndex]
                shuffled[swapIndex] = i - 1
            }

            for (i in (firstIndex + 1) until length) {
                val swapIndex = random.nextInt(1, i + 1)
                shuffled[i] = shuffled[swapIndex]
                shuffled[swapIndex] = i
            }
        }

        return shuffled
    }
}