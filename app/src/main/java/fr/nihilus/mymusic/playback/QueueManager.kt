package fr.nihilus.mymusic.playback

import android.os.Bundle
import android.support.annotation.VisibleForTesting
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import fr.nihilus.mymusic.di.MusicServiceScope
import fr.nihilus.mymusic.media.MusicRepository
import fr.nihilus.mymusic.service.MusicService
import fr.nihilus.mymusic.utils.MediaID
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import java.util.*
import javax.inject.Inject

private const val TAG = "QueueManager"

@MusicServiceScope
class QueueManager
@Inject constructor(
        service: MusicService,
        repository: MusicRepository
) {
    private val mResources = service.resources
    private val mListener: MetadataUpdateListener = service
    private val mRepository = repository

    private val mPlayingQueue: MutableList<MediaSessionCompat.QueueItem> = ArrayList()
    private var mCurrentIndex = 0

    /**
     * Item in the queue that is currently selected.
     * When the playback starts or resume, this item will be the one to be played.
     */
    val currentMusic: MediaSessionCompat.QueueItem?
        get() = if (isIndexPlayable(mCurrentIndex, mPlayingQueue)) mPlayingQueue[mCurrentIndex] else null

    /**
     * Indicates if a [mediaId] belongs to the same hierarchy
     * as the one whose items are loaded into the queue.
     */
    @VisibleForTesting
    fun isSameBrowsingCategory(mediaId: String): Boolean {
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
     * @param steps the number of items to skip, negative is backward, position is forward
     * @return whether the move succeeded
     */
    fun skipPosition(steps: Int): Boolean {
        // TODO Implement cycling capabilities
        var index = mCurrentIndex + steps
        index = if (index < 0) 0 else index % mPlayingQueue.size

        if (!isIndexPlayable(index, mPlayingQueue)) {
            Log.e(TAG, "Cannot increment queue index by $steps. Current=$mCurrentIndex, " +
                    "queue length= ${mPlayingQueue.size}")
            return false
        }

        mCurrentIndex = index
        return true
    }

    fun loadQueueFromSearch(query: String, extras: Bundle?): Boolean {
        TODO("Implement search logic in MusicRepository and retrieve queue from it")
    }

    /**
     *
     */
    fun loadQueueFromMusic(mediaId: String, shuffled: Boolean = false) {
        Log.d(TAG, "loadQueueFromMusic: $mediaId")
        val canReuseQueue = isSameBrowsingCategory(mediaId) && setCurrentQueueItem(mediaId)
        if (!canReuseQueue) {
            // TODO Determine queue name from MediaId, get static names from resources
            val queueTitle = "Playing queue"
            mRepository.getMediaChildren(mediaId).subscribe { medias: List<MediaDescriptionCompat> ->
                setCurrentQueue(queueTitle, medias.mapIndexed {
                    index, descr -> MediaSessionCompat.QueueItem(descr, index.toLong())
                })
                updateMetadata()
            }
        }

        updateMetadata()
    }

    fun updateMetadata() {
        val currentMusic = currentMusic
        if (currentMusic == null) {
            mListener.onMetadataRetrieveError()
            return
        }

        val musicId = MediaID.extractMusicID(currentMusic.description.mediaId)
                ?: throw IllegalStateException("Queue item should have a media ID")

        // TODO Chain with another Single to load album art if needed
        mRepository.getMetadata(musicId.toLong()).subscribe(object : SingleObserver<MediaMetadataCompat> {
            override fun onSubscribe(d: Disposable) {}
            override fun onSuccess(metadata: MediaMetadataCompat) = mListener.onMetadataChanged(metadata)
            override fun onError(e: Throwable) = mListener.onMetadataRetrieveError()
        })
    }

    fun loadRandomQueue() {
        TODO("Should be the same as loadQueueFromMusic, but in random order")
    }

    @VisibleForTesting
    internal fun setCurrentQueue(title: String, newQueue: List<MediaSessionCompat.QueueItem>,
                                initialMediaId: String? = null) {
        mPlayingQueue.clear()
        mPlayingQueue.addAll(newQueue)
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