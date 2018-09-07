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

package fr.nihilus.music.media.playback

import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.ShuffleOrder
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import fr.nihilus.music.media.assert
import fr.nihilus.music.media.*
import fr.nihilus.music.media.command.MediaSessionCommand
import fr.nihilus.music.media.di.ServiceScoped
import fr.nihilus.music.media.repo.MusicRepository
import fr.nihilus.music.media.service.AlbumArtLoader
import fr.nihilus.music.media.service.MusicService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

@ServiceScoped
class MediaQueueManager
@Inject constructor(
    service: MusicService,
    private val mediaSession: MediaSessionCompat,
    private val prefs: MediaSettings,
    private val repository: MusicRepository,
    private val player: ExoPlayer,
    private val iconLoader: AlbumArtLoader,
    private val commands: Map<String, @JvmSuppressWildcards MediaSessionCommand>

) : MediaSessionConnector.PlaybackPreparer,
    MediaSessionConnector.QueueNavigator {

    private val mediaSourceFactory: ExtractorMediaSource.Factory
    private val currentQueue = ArrayList<MediaDescriptionCompat>()

    private var lastMusicId: String? = null

    private val navigator = object : TimelineQueueNavigator(mediaSession) {
        override fun getMediaDescription(
            player: Player?,
            windowIndex: Int
        ): MediaDescriptionCompat {
            assert(windowIndex in currentQueue.indices)
            return currentQueue[windowIndex]
        }
    }

    init {
        val userAgent = Util.getUserAgent(service, service.getString(R.string.app_name))
        val dataSourceFactory = DefaultDataSourceFactory(service, userAgent)
        mediaSourceFactory = ExtractorMediaSource.Factory(dataSourceFactory)
            .setExtractorsFactory(AudioOnlyExtractorsFactory())
    }

    override fun getSupportedPrepareActions(): Long {
        // TODO Update supported action codes to include *_FROM_SEARCH
        return PlaybackStateCompat.ACTION_PREPARE or
                PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
    }

    /**
     * Fixes a bug in ExoPlayer 2.8.1 where skipping to previous/next media
     * in a in a [ConcatenatingMediaSource] cannot be performed when in shuffle mode.
     */
    override fun getSupportedQueueNavigatorActions(player: Player?): Long {
        if (player == null || player.currentTimeline.windowCount < 2) {
            return 0L
        }

        if (player.repeatMode != Player.REPEAT_MODE_OFF) {
            return PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM
        }

        return PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM or when {
            player.previousWindowIndex == C.INDEX_UNSET -> PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            player.nextWindowIndex == C.INDEX_UNSET -> PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            else -> PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        }
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
        val lastPlayedMediaId = prefs.lastPlayedMediaId ?: CATEGORY_MUSIC
        prepareFromMediaId(lastPlayedMediaId, shuffled = false)
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
        prefs.queueCounter++
        val shuffleExtraEnabled = extras?.getBoolean(Constants.EXTRA_PLAY_SHUFFLED) ?: false
        prepareFromMediaId(mediaId ?: CATEGORY_MUSIC, shuffleExtraEnabled)
    }

    override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) {
        // Not supported at the time.
        throw UnsupportedOperationException()
    }

    override fun onPrepareFromSearch(query: String?, extras: Bundle?) {
        TODO("Implement the searching API.")
    }

    /**
     * Called when skipping to the previous song in timeline.
     * When repeat mode is REPEAT_ONE, allow skipping to previous item by temporarily setting
     * player's repeat mode to REPEAT_ALL.
     */
    override fun onSkipToPrevious(player: Player?) {
        if (player != null && player.repeatMode == Player.REPEAT_MODE_ONE) {
            player.repeatMode = Player.REPEAT_MODE_ALL
            navigator.onSkipToPrevious(player)
            player.repeatMode = Player.REPEAT_MODE_ONE
        } else {
            navigator.onSkipToPrevious(player)
        }
    }

    /**
     * Called when skipping to the next song in timeline.
     * When repeat mode is REPEAT_ONE, allow skipping to next item by temporarily setting
     * player's repeat mode to REPEAT_ALL.
     */
    override fun onSkipToNext(player: Player?) {
        if (player != null && player.repeatMode == Player.REPEAT_MODE_ONE) {
            player.repeatMode = Player.REPEAT_MODE_ALL
            navigator.onSkipToNext(player)
            player.repeatMode = Player.REPEAT_MODE_ONE
        } else {
            navigator.onSkipToNext(player)
        }
    }

    override fun onSkipToQueueItem(player: Player?, id: Long) {
        navigator.onSkipToQueueItem(player, id)
    }

    override fun onCurrentWindowIndexChanged(player: Player) {
        navigator.onCurrentWindowIndexChanged(player)
        onUpdateMediaSessionMetadata(player)
    }

    override fun getActiveQueueItemId(player: Player?): Long {
        return navigator.getActiveQueueItemId(player)
    }

    override fun onTimelineChanged(player: Player?) {
        navigator.onTimelineChanged(player)
    }

    override fun getCommands() = commands.keys.toTypedArray()

    override fun onCommand(player: Player?, command: String?,
                           extras: Bundle?, cb: ResultReceiver?) {
        commands[command]?.handle(extras, cb)
                ?: cb?.send(R.id.abc_error_unknown_command, null)
    }

    private fun prepareFromMediaId(mediaId: String, shuffled: Boolean) {
        repository.getMediaItems(mediaId).subscribe { items ->
            currentQueue.clear()

            val playableItems = items.filterNot { it.isBrowsable }
                .mapTo(currentQueue) { it.description }
            val mediaSources = Array(playableItems.size) {
                val sourceUri = checkNotNull(playableItems[it].mediaUri) {
                    "Every item should have an Uri."
                }

                mediaSourceFactory.createMediaSource(sourceUri)
            }

            // Defines a shuffle order for the loaded media sources that is predictable.
            // It depends on the number of time a new queue has been built.
            val predictableShuffleOrder = ShuffleOrder.DefaultShuffleOrder(
                mediaSources.size,
                prefs.queueCounter
            )

            // Concatenate all media source to play them all in the same Timeline.
            val concatenatedSource = ConcatenatingMediaSource(false,
                predictableShuffleOrder,
                *mediaSources
            )

            player.prepare(concatenatedSource)

            // Start at a given track if it is mentioned in the passed media id.
            val startIndex = playableItems.indexOfFirst { it.mediaId == mediaId }
            if (startIndex != -1) {
                player.seekTo(startIndex, C.TIME_UNSET)
            }

            player.shuffleModeEnabled = shuffled || player.shuffleModeEnabled
        }
    }

    private fun onUpdateMediaSessionMetadata(player: Player) {
        val currentWindowIndex = player.currentWindowIndex
        val activeItem = currentQueue[currentWindowIndex]
        val activeMediaId = activeItem.mediaId
        prefs.lastPlayedMediaId = activeMediaId
        val musicId = checkNotNull(musicIdFrom(activeMediaId)) {
            "Each playable track should have a music ID"
        }

        if (lastMusicId != musicId) {
            // Only update metadata if it has really changed.
            repository.getMetadata(musicId)
                .subscribeOn(Schedulers.io())
                .flatMap { iconLoader.loadIntoMetadata(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaSession::setMetadata)
        }

        // Remember the last change in metadata
        lastMusicId = musicId
    }
}