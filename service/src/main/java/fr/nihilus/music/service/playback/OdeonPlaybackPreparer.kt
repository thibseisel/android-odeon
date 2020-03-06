/*
 * Copyright 2019 Thibault Seisel
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
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.ShuffleOrder
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import fr.nihilus.music.core.context.AppDispatchers
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.toMediaId
import fr.nihilus.music.core.os.PermissionDeniedException
import fr.nihilus.music.core.settings.Settings
import fr.nihilus.music.media.R
import fr.nihilus.music.service.browser.BrowserTree
import fr.nihilus.music.service.browser.SearchQuery
import fr.nihilus.music.service.extensions.doOnPrepared
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
internal class OdeonPlaybackPreparer
@Inject constructor(
    context: Context,
    private val scope: CoroutineScope,
    private val dispatchers: AppDispatchers,
    private val player: ExoPlayer,
    private val browserTree: BrowserTree,
    private val settings: Settings
) : MediaSessionConnector.PlaybackPreparer {

    private val audioOnlyExtractors = AudioOnlyExtractorsFactory()
    private val appDataSourceFactory = DefaultDataSourceFactory(
        context,
        Util.getUserAgent(context, context.getString(R.string.core_app_name))
    )

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
    override fun onPrepare() {
        // Should prepare playing the "current" media, which is the last played media id.
        // If not available, play all songs.
        val lastPlayedMediaId = settings.lastQueueMediaId ?: MediaId.ALL_TRACKS
        prepareFromMediaId(lastPlayedMediaId, settings.lastQueueIndex)
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
        // A new queue has been requested. Update the last played queue media id (the queue identifier will change).
        val queueMediaId = mediaId ?: MediaId.ALL_TRACKS
        settings.lastQueueMediaId = queueMediaId

        prepareFromMediaId(queueMediaId, C.POSITION_UNSET)
    }

    override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) {
        // Not supported at the time.
        throw UnsupportedOperationException()
    }

    /**
     * Handle requests to prepare for playing tracks picked from the results of a search.
     */
    @SuppressLint("LogNotTimber")
    override fun onPrepareFromSearch(query: String?, extras: Bundle?) {
        // TODO Remove those lines when got enough info on how Assistant understands voice searches.
        if (Log.isLoggable("AssistantSearch", Log.INFO)) {
            val extString = extras?.keySet()
                ?.joinToString(", ", "{", "}") { "$it=${extras[it]}" }
                ?: "null"
            Log.i("AssistantSearch", "onPrepareFromSearch: query=\"$query\", extras=$extString")
        }

        val parsedQuery = SearchQuery.from(query, extras)
        if (parsedQuery is SearchQuery.Empty) {
            // Generic query, such as "play music"
            onPrepare()

        } else scope.launch(dispatchers.Default) {
            val results = browserTree.search(parsedQuery)

            val firstResult = results.firstOrNull()
            if (firstResult?.isBrowsable == true) {
                prepareFromMediaId(firstResult.mediaId, C.POSITION_UNSET)
            } else {
                preparePlayer(
                    results.filter { it.isPlayable && !it.isBrowsable },
                    firstShuffledIndex = 0,
                    startPosition = 0
                )
            }
        }
    }

    override fun getCommands(): Array<String>? = null

    override fun onCommand(
        player: Player?,
        command: String?,
        extras: Bundle?,
        cb: ResultReceiver?
    ) = Unit

    private suspend fun loadPlayableChildrenOf(parentId: MediaId): List<MediaItem> = try {
        val children = browserTree.getChildren(parentId, null).first()
        children.filter { it.isPlayable && !it.isBrowsable }

    } catch (pde: PermissionDeniedException) {
        Timber.i("Unable to load children of %s: denied permission %s", parentId, pde.permission)
        emptyList()

    } catch (invalidParent: NoSuchElementException) {
        Timber.i("Unable to load children of %s: not a browsable item from the tree", parentId)
        emptyList()
    }

    private fun prepareFromMediaId(
        mediaId: String?,
        startPlaybackPosition: Int
    ) = scope.launch(dispatchers.Default) {
        val (type, category, track) = mediaId.toMediaId()
        val parentId = MediaId.fromParts(type, category, track = null)

        val playQueue = loadPlayableChildrenOf(parentId)
        val firstIndex = if (track != null) {
            playQueue.indexOfFirst { it.mediaId == mediaId }
        } else C.POSITION_UNSET
        preparePlayer(playQueue, firstIndex, startPlaybackPosition)
    }

    /**
     * Prepare playback of a given [playQueue]
     * and start playing the index at [startPosition] when ready.
     *
     * @param playQueue The items to be played. All media should be playable and have a media uri.
     * @param firstShuffledIndex The index of the item that should be the first when playing shuffled.
     * This should be a valid index in [playQueue], otherwise an index is chosen randomly.
     * @param startPosition The index of the item that should be played when the player is ready.
     * This should be a valid index in [playQueue],
     * otherwise playback will be set to start at the first index in the queue (shuffled or not).
     */
    private suspend fun preparePlayer(
        playQueue: List<MediaItem>,
        firstShuffledIndex: Int,
        startPosition: Int
    ) {
        if (playQueue.isNotEmpty()) withContext(dispatchers.Main) {
            val mediaSources = Array(playQueue.size) {
                val playableItem = playQueue[it].description
                val sourceUri = checkNotNull(playableItem.mediaUri) {
                    "Track ${playableItem.mediaId} (${playableItem.title} should have a media Uri."
                }

                ExtractorMediaSource.Factory(appDataSourceFactory)
                    .setExtractorsFactory(audioOnlyExtractors)
                    .setTag(playableItem)
                    .createMediaSource(sourceUri)
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

            // Concatenate all media source to play them all in the same Timeline.
            val concatenatedSource = ConcatenatingMediaSource(
                false,
                predictableShuffleOrder,
                *mediaSources
            )

            // Prepare the new playing queue.
            // Because of an issue with ExoPlayer, shuffle order is reset when player is prepared.
            // As a workaround, wait for the player to be prepared before setting the shuffle order.
            player.prepare(concatenatedSource)
            player.doOnPrepared {
                concatenatedSource.setShuffleOrder(predictableShuffleOrder)
            }

            // Start playback at a given position if specified, otherwise at first shuffled index.
            val targetPlaybackPosition = when (startPosition) {
                in playQueue.indices -> startPosition
                else -> predictableShuffleOrder.firstIndex
            }

            player.seekToDefaultPosition(targetPlaybackPosition)
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