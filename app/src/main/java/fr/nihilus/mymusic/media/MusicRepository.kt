package fr.nihilus.mymusic.media

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.annotation.VisibleForTesting
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.util.LongSparseArray
import fr.nihilus.mymusic.utils.MediaID
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A Repository that centralize access to media stored on the device.
 * You must initialize the repository with [init] before querying items.
 * Observables returned by query methods will not emit items until properly initialized.
 */
@Singleton
class MusicRepository
@Inject internal constructor(mediaDao: MediaDao) {

    private val metadataById = LongSparseArray<MediaMetadataCompat>()
    private val metadatas = mediaDao.getAllTracks()
            .doOnNext(this::cacheMetadatas)
            .share()

    /**
     * Fetch [MediaItem]s children of a given Media ID.
     * The returned [Observable] will emit the requested children or an error if [parentMediaId] is unsupported.
     * @param parentMediaId
     * @return an observable list of media items proper for display.
     */
    fun getMediaItems(parentMediaId: String): Single<List<MediaItem>> {
        return when (parentMediaId) {
            MediaID.ID_MUSIC -> metadatas.first(emptyList()).map(this::asMediaItems)
            else -> Single.error(::UnsupportedOperationException)
        }
    }

    /**
     * @return a single metadata item with the specified musicId
     */
    fun getMetadata(musicId: String): Single<MediaMetadataCompat> {
        val fromCache = Observable.fromCallable { this.metadataById[musicId.toLong()] }
        TODO("Search in cache, or in DAO if not found")
    }

    /**
     * Convert a list of [MediaMetadataCompat]s into a list of [MediaItem]s with the MUSIC media ID.
     */
    private fun asMediaItems(metadataList: List<MediaMetadataCompat>): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()
        return metadataList.map { it.asMediaItem(MediaID.ID_MUSIC, builder) }
    }

    /**
     * An Observable that notify observers when a change is detected in the music library.
     * It emits the parent media id of the collection of items that have change.
     *
     * Clients should then call [getMediaItems] to get up to date items.
     *
     * When done observing changes, clients __must__ dispose their observers
     * through [io.reactivex.disposables.Disposable.dispose] to avoid memory leaks.
     */
    val mediaChanges: Observable<String> by lazy {
        // TODO Add any other media that are subject to changes
        metadatas.map { _ -> MediaID.ID_MUSIC }
    }

    /**
     * Release all references to objects loaded in this repository.
     * You'll have to [init] this repository again to use it.
     */
    fun clear() {
        this.metadataById.clear()
    }

    private fun cacheMetadatas(metadataList: List<MediaMetadataCompat>) {
        for (meta in metadataList) {
            val musicId = meta.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID).toLong()
            metadataById.put(musicId, meta)
        }
    }
}

/**
 * Convert this [MediaMetadataCompat] into its [MediaItem] equivalent.
 * @param parentMediaId the Media ID to use a prefix for this item's Media ID
 * @param builder an optional builder for reuse
 * @return a media item created from this track metadatas
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal fun MediaMetadataCompat.asMediaItem(
        parentMediaId: String,
        builder: MediaDescriptionCompat.Builder = MediaDescriptionCompat.Builder()
): MediaItem {

    val musicId = this.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
    val extras = Bundle(2)
    extras.putString(MediaItems.EXTRA_TITLE_KEY, this.getString(MediaDao.CUSTOM_META_TITLE_KEY))
    extras.putLong(MediaItems.EXTRA_DURATION, this.getLong(MediaMetadataCompat.METADATA_KEY_DURATION))
    val artUri = this.getString(MediaMetadataCompat.METADATA_KEY_ART_URI)

    builder.setMediaId(MediaID.createMediaID(musicId, parentMediaId))
            .setTitle(this.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
            .setSubtitle(this.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
            .setMediaUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon()
                    .appendEncodedPath(musicId)
                    .build())
            .setExtras(extras)
    artUri?.let { builder.setIconUri(Uri.parse(it)) }

    return MediaItem(builder.build(), MediaItem.FLAG_PLAYABLE)
}
