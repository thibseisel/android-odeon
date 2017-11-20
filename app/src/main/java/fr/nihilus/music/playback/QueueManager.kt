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

package fr.nihilus.music.playback

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import fr.nihilus.music.di.ServiceScoped
import fr.nihilus.music.media.repo.MusicRepository
import fr.nihilus.music.service.AlbumArtLoader
import fr.nihilus.music.service.MusicService
import fr.nihilus.music.service.VoiceSearchParams
import fr.nihilus.music.utils.MediaID
import io.reactivex.Observable
import java.util.*
import javax.inject.Inject

private const val TAG = "QueueManager"

@ServiceScoped
class QueueManager
@Inject constructor(
        service: MusicService,
        private val artLoader: AlbumArtLoader,
        private val repository: MusicRepository
) {
    private val mResources = service.resources
    private val mListener: MetadataUpdateListener = service

    private val mPlayingQueue: MutableList<MediaSessionCompat.QueueItem> = ArrayList()
    private var mCurrentIndex = 0

    @PlaybackStateCompat.ShuffleMode
    var shuffleMode: Int = PlaybackStateCompat.SHUFFLE_MODE_NONE
        set(value) {
            if (field != value) {
                field = value
                val currentMediaId = currentMusic?.description?.mediaId
                setCurrentQueue(getQueueTitle(currentMediaId), mPlayingQueue, currentMediaId)
            }
        }

    /**
     * Item in the queue that is currently selected.
     * When the playback starts or resume, this item will be the one to be played.
     */
    val currentMusic: MediaSessionCompat.QueueItem?
        get() = if (isIndexPlayable(mCurrentIndex, mPlayingQueue))
            mPlayingQueue[mCurrentIndex]
        else null

    /**
     * Indicates if a [mediaId] belongs to the same hierarchy
     * as the one whose items are loaded into the queue.
     */
    internal fun isSameBrowsingCategory(mediaId: String): Boolean {
        val newBrowseHierarchy = MediaID.getHierarchy(mediaId)
        val current = currentMusic ?: return false

        val currentBrowseHierarchy = MediaID.getHierarchy(current.description.mediaId!!)
        return Arrays.equals(newBrowseHierarchy, currentBrowseHierarchy)
    }

    private fun setCurrentQueueIndex(index: Int) {
        if (isIndexPlayable(index, mPlayingQueue)) {
            mCurrentIndex = index
            mListener.onCurrentQueueIndexUpdated(index)
        }
    }

    /**
     * Move to a specific item in the queue.
     * @param queueId unique identifier of the item
     * @return whether changing current queue item was successful
     */
    fun setCurrentQueueItem(queueId: Long): Boolean {
        val index = musicIndexOnQueue(mPlayingQueue, queueId)
        setCurrentQueueIndex(index)
        return index >= 0
    }

    private fun setCurrentQueueItem(mediaId: String): Boolean {
        val index = musicIndexOnQueue(mPlayingQueue, mediaId)
        setCurrentQueueIndex(index)
        return index >= 0
    }

    /**
     * Move in the queue relatively to the current position.
     * If the number of steps is negative, then the move is backward.
     *
     * If attempted to skip backwards before the first song, then move to the first song.
     * If attempted to skip forwards after the last song, then move to the last song.
     *
     * @param steps the number of items to skip, negative is backward, position is forward
     * @return whether the move succeeded
     */
    fun skipPosition(steps: Int): Boolean {
        var index = mCurrentIndex + steps
        index = index.coerceIn(0, mPlayingQueue.size)

        if (!isIndexPlayable(index, mPlayingQueue)) {
            Log.e(TAG, """
                Cannot increment queue index by $steps steps.
                Current index = $mCurrentIndex, queue length = ${mPlayingQueue.size}
                This was never supposed to happen.
                """.trimIndent())
            return false
        }

        mCurrentIndex = index
        return true
    }

    fun canSkip(steps: Int): Boolean = isIndexPlayable(mCurrentIndex + steps, mPlayingQueue)

    fun loadQueueFromSearch(query: String?, extras: Bundle?): Boolean {

        val params = VoiceSearchParams(query, extras)
        if (params.isAny) {
            loadQueueFromMusic(MediaID.ID_RANDOM)
            return true
        }

        TODO("Should be best to handle that in a search method of MusicRepository")
    }

    /**
     *
     */
    fun loadQueueFromMusic(mediaId: String) {
        Log.d(TAG, "loadQueueFromMusic: $mediaId")
        val canReuseQueue = if (isSameBrowsingCategory(mediaId))
            setCurrentQueueItem(mediaId) else false

        if (!canReuseQueue) {
            // TODO Determine queue name from MediaId, get static names from resources
            val queueTitle = getQueueTitle(mediaId)

            // When using Schedulers, playback is launched for last played media ID
            // before the queue is fully loaded.
            // A solution might be to return a Completable from this method.

            repository.getMediaItems(mediaId).toObservable()
                    .flatMap { Observable.fromIterable(it) }
                    .filter { !it.isBrowsable }
                    .toList()
                    .subscribe { medias: List<MediaBrowserCompat.MediaItem> ->
                        Log.d(TAG, "MediaID=$mediaId, size=${medias.size}")
                        setCurrentQueue(queueTitle, medias.mapIndexed { index, item ->
                            MediaSessionCompat.QueueItem(item.description, index.toLong())
                        }, mediaId)
                        updateMetadata()
                    }
        } else {
            updateMetadata()
        }

    }

    fun updateMetadata() {
        val currentMusic = currentMusic
        if (currentMusic == null) {
            mListener.onMetadataRetrieveError()
            return
        }

        val musicId = MediaID.extractMusicID(currentMusic.description.mediaId)
                ?: throw IllegalStateException("Queue item should have a media ID")

        repository.getMetadata(musicId)
                .flatMap(artLoader::loadIntoMetadata)
                .subscribe(mListener::onMetadataChanged)
                { mListener.onMetadataRetrieveError() }
    }

    private fun getQueueTitle(mediaId: String?): String {
        // TODO Retrieve queue title from media id
        return "Playing queue"
    }

    internal fun setCurrentQueue(title: String, newQueue: List<MediaSessionCompat.QueueItem>,
                                 initialMediaId: String? = null) {
        // Clear queue only if its content is not the same
        if (mPlayingQueue !== newQueue) {
            mPlayingQueue.clear()
            mPlayingQueue.addAll(newQueue)
        }

        if (shuffleMode != PlaybackStateCompat.SHUFFLE_MODE_NONE)
            Collections.shuffle(mPlayingQueue)
        else mPlayingQueue.sortBy { it.queueId }

        val index = if (initialMediaId != null) musicIndexOnQueue(mPlayingQueue, initialMediaId) else 0
        mCurrentIndex = maxOf(0, index)
        mListener.onQueueUpdated(title, newQueue)
    }

    interface MetadataUpdateListener {
        fun onMetadataChanged(metadata: MediaMetadataCompat)
        fun onMetadataRetrieveError()
        fun onCurrentQueueIndexUpdated(queueIndex: Int)
        fun onQueueUpdated(title: String, newQueue: List<MediaSessionCompat.QueueItem>)
    }
}