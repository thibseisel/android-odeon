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
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.ShuffleOrder
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import fr.nihilus.music.media.command.MediaSessionCommand
import fr.nihilus.music.media.playback.AudioOnlyExtractorsFactory
import fr.nihilus.music.media.repo.MusicRepository
import fr.nihilus.music.media.service.MusicService
import fr.nihilus.music.media.utils.plusAssign
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import javax.inject.Inject
import kotlin.random.Random

/**
 * Handle requests to prepare media that can be played from the Odeon Media Player.
 * This fetches media information from the music library.
 */
internal class OdeonPlaybackPreparer
@Inject constructor(
    service: MusicService,
    private val player: ExoPlayer,
    private val repository: MusicRepository,
    private val settings: MediaSettings,
    private val commandHandlers: Map<String, @JvmSuppressWildcards MediaSessionCommand>,
    private val subscriptions: CompositeDisposable
) : MediaSessionConnector.PlaybackPreparer {

    private val audioOnlyExtractors = AudioOnlyExtractorsFactory()
    private val appDataSourceFactory = DefaultDataSourceFactory(
        service,
        Util.getUserAgent(service, service.getString(R.string.app_name))
    )

    override fun getSupportedPrepareActions(): Long {
        // TODO Update supported action codes to include *_FROM_SEARCH
        return PlaybackStateCompat.ACTION_PREPARE or
                PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
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
        val lastPlayedMediaId = settings.lastPlayedMediaId ?: CATEGORY_MUSIC
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
        prepareFromMediaId(mediaId ?: CATEGORY_MUSIC)
    }

    override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) {
        // Not supported at the time.
        throw UnsupportedOperationException()
    }

    override fun onPrepareFromSearch(query: String?, extras: Bundle?) {
        // TODO: Implement searching for Google Assistant
        throw UnsupportedOperationException()
    }

    override fun getCommands(): Array<String> = commandHandlers.keys.toTypedArray()

    override fun onCommand(player: Player?, command: String?, extras: Bundle?, cb: ResultReceiver?) {
        commandHandlers[command]?.handle(extras, cb)
                ?: cb?.send(R.id.abc_error_unknown_command, null)
    }

    private fun prepareFromMediaId(mediaId: String) {
        subscriptions += repository.getMediaItems(mediaId).subscribe(
            { onMediaItemsLoaded(mediaId, it) },
            { Timber.i(it, "Error while preparing queue.") }
        )
    }

    private fun onMediaItemsLoaded(mediaId: String, items: List<MediaBrowserCompat.MediaItem>) {
        val playableItems = items.asSequence()
            .filterNot { it.isBrowsable }
            .map { it.description }
            .toList()

        // Short-circuit: if there are no playable items.
        if (playableItems.isEmpty()) {
            return
        }

        val mediaSources = Array(playableItems.size) {
            val playableItem: MediaDescriptionCompat = playableItems[it]
            val sourceUri = checkNotNull(playableItem.mediaUri) {
                "Every item should have an Uri."
            }

            ExtractorMediaSource.Factory(appDataSourceFactory)
                .setExtractorsFactory(audioOnlyExtractors)
                .setTag(playableItem)
                .createMediaSource(sourceUri)
        }

        val queueIdentifier = settings.queueCounter
        val firstIndex = playableItems.indexOfFirst { it.mediaId == mediaId }

        val shuffleOrder = if (firstIndex == -1) {
            ShuffleOrder.DefaultShuffleOrder(mediaSources.size, queueIdentifier)
        } else {
            val shuffledIndices = createShuffledIndices(firstIndex, mediaSources.size, queueIdentifier)
            ShuffleOrder.DefaultShuffleOrder(shuffledIndices, queueIdentifier)
        }

        // Concatenate all media source to play them all in the same Timeline.
        val concatenatedSource = ConcatenatingMediaSource(false, shuffleOrder, *mediaSources)
        player.prepare(concatenatedSource)
    }

    /**
     * Create a sequence consisting of consecutive numbers in `[0 ; length[` in shuffled order,
     * starting by the given [firstIndex].
     *
     * @param firstIndex The first value in the produces array.
     * @param length The length of the produced array.
     * @param randomSeed The seed for shuffling numbers.
     */
    private fun createShuffledIndices(firstIndex: Int, length: Int, randomSeed: Long): IntArray {
        val random = Random(randomSeed)
        val shuffled = IntArray(length)
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

        return shuffled
    }
}