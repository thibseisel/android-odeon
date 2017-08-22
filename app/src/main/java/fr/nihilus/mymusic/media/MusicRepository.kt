package fr.nihilus.mymusic.media

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.annotation.VisibleForTesting
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.util.LongSparseArray
import fr.nihilus.mymusic.MetadataList
import fr.nihilus.mymusic.utils.MediaID
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MusicRepository"

/**
 * A Repository that centralize access to media stored on the device.
 * It returns [MediaItem]s depending on a specified parent media ID.
 * When in use, it is recommanded to listen for the [mediaChanges] property to be notified when
 * a subset of item has changed.
 */
@Singleton
open class MusicRepository
@Inject internal constructor(private val mediaDao: MediaDao) {

    private val metadataById = LongSparseArray<MediaMetadataCompat>()
    private val metadatas = mediaDao.getAllTracks()
            .doOnNext { cacheMetadatas(it) }
            .share()

    /**
     * Fetch [MediaDescriptionCompat]s children of a given Media ID.
     * The returned [Observable] will emit the requested children or an error if [parentMediaId] is unsupported.
     * @param parentMediaId the media id that identifies the requested medias
     * @return an observable list of media descriptions proper for display.
     */
    open fun getMediaChildren(parentMediaId: String): Single<List<MediaDescriptionCompat>> {
        val parentHierarchy = MediaID.getHierarchy(parentMediaId)
        return when (parentHierarchy[0]) {
            MediaID.ID_MUSIC -> metadatas.first(emptyList()).map(this::toMediaDescriptions)
            MediaID.ID_ALBUMS -> {
                if (parentHierarchy.size > 1) getAlbumTracks(parentHierarchy[1].toLong())
                        .map(this::toMediaDescriptions)
                else mediaDao.getAlbums().firstOrError()
            }
            MediaID.ID_ARTISTS -> {
                if (parentHierarchy.size > 1) getArtistItems(parentHierarchy[1].toLong())
                else mediaDao.getArtists().firstOrError()
            }
            else -> Single.error(::UnsupportedOperationException)
        }
    }

    fun getMediaItems(parentMediaId: String): Single<List<MediaItem>> {
        // Get the "true" parent in case the passed media id is a playable item
        val trueParent = MediaID.stripMusicId(parentMediaId)
        val parentHierarchy = MediaID.getHierarchy(trueParent)
        return when (parentHierarchy[0]) {
            MediaID.ID_MUSIC -> metadatas.first(emptyList())
                    .map { toMediaItems(trueParent, it) }
            MediaID.ID_ALBUMS -> {
                if (parentHierarchy.size > 1) getAlbumTracks(parentHierarchy[1].toLong())
                        .map { toMediaItems(trueParent, it) }
                else mediaDao.getAlbums()
                        .flatMap { Observable.fromIterable(it) }
                        .map { MediaItem(it, MediaItem.FLAG_BROWSABLE) }
                        .toList()
            }
            else -> Single.error(::UnsupportedOperationException)
        }
    }

    /**
     * Build a playing queue composed of children of a given Media ID.
     * The returned [Observable] will emit the requested items or an error if [parentMediaId] is unsupported.
     * @param parentMediaId the media id that identifies the requested medias
     * @return an observable list of items that can be played
     */
    fun getQueueChildren(parentMediaId: String): Single<List<MediaSessionCompat.QueueItem>> {
        val trueParent = MediaID.stripMusicId(parentMediaId)
        val parentHierarchy = MediaID.getHierarchy(trueParent)
        return when (parentHierarchy[0]) {
            MediaID.ID_MUSIC -> metadatas.first(emptyList())
                    .map { toQueueItems(MediaID.ID_MUSIC, it) }
            MediaID.ID_ALBUMS -> getAlbumTracks(parentHierarchy[1].toLong())
                    .map { toQueueItems(trueParent, it) }
            //MediaID.ID_ARTISTS -> mediaDao.getArtistTracks(parentHierarchy[1].toLong())
            else -> Single.error(::UnsupportedOperationException)
        }
    }

    private fun getArtistItems(artistId: Long): Single<List<MediaDescriptionCompat>> {
        val albums = mediaDao.getArtistAlbums(artistId)
        val tracks = mediaDao.getArtistTracks(artistId).map(this::toMediaDescriptions)
        return Observable.concat(albums, tracks)
                .flatMap { Observable.fromIterable(it) }
                .toList()
    }

    /**
     * @return a single metadata item with the specified musicId
     */
    open fun getMetadata(musicId: Long): Single<MediaMetadataCompat> {
        val fromCache = Single.fromCallable {
            metadataById.get(musicId) ?: throw RuntimeException("No metadata with ID $musicId")
        }

        // TODO Fetch from MediaDao if missing
        return fromCache
    }

    private fun getAlbumTracks(albumId: Long): Single<MetadataList> {
        // TODO Get tracks from cache if exist
        return mediaDao.getAlbumTracks(albumId).single(emptyList())
    }

    /**
     * Convert a list of [MediaMetadataCompat]s into a list of [MediaDescriptionCompat]s with the MUSIC media ID.
     */
    private fun toMediaDescriptions(metadataList: List<MediaMetadataCompat>): List<MediaDescriptionCompat> {
        val builder = MediaDescriptionCompat.Builder()
        return metadataList.map { it.asMediaDescription(MediaID.ID_MUSIC, builder) }
    }

    private fun toQueueItems(parentMediaId: String, metadataList: List<MediaMetadataCompat>)
            : List<MediaSessionCompat.QueueItem> {
        val builder = MediaDescriptionCompat.Builder()
        return metadataList
                .map { it.asMediaDescription(parentMediaId, builder) }
                .mapIndexed { index, media -> MediaSessionCompat.QueueItem(media, index.toLong()) }
    }

    private fun toMediaItems(parentMediaId: String, metadataList: List<MediaMetadataCompat>)
            : List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()
        return metadataList
                .map { it.asMediaDescription(parentMediaId, builder) }
                .map { description -> MediaItem(description, MediaItem.FLAG_PLAYABLE) }
    }

    /**
     * An Observable that notify observers when a change is detected in the music library.
     * It emits the parent media id of the collection of items that have change.
     *
     * Clients are then advised to call [getMediaChildren] to fetch up-to-date items.
     *
     * When done observing changes, clients __must__ dispose their observers
     * through [Disposable.dispose] to avoid memory leaks.
     */
    open val mediaChanges: Observable<String> by lazy {
        // TODO Add any other media that are subject to changes
        metadatas.map { _ -> MediaID.ID_MUSIC }
    }

    /**
     * Release all references to objects loaded by this repository.
     */
    open fun clear() {
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
 * Convert this [MediaMetadataCompat] into its [MediaDescriptionCompat] equivalent.
 * @param parentMediaId the Media ID to use a prefix for this item's Media ID
 * @param builder an optional builder for reuse
 * @return a media description created from this track metadatas
 */
@VisibleForTesting
internal fun MediaMetadataCompat.asMediaDescription(
        parentMediaId: String,
        builder: MediaDescriptionCompat.Builder = MediaDescriptionCompat.Builder()
): MediaDescriptionCompat {
    val musicId = getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
    val extras = Bundle(3)
    extras.putString(MediaItems.EXTRA_TITLE_KEY, getString(MediaDao.CUSTOM_META_TITLE_KEY))
    extras.putLong(MediaItems.EXTRA_DURATION, getLong(MediaMetadataCompat.METADATA_KEY_DURATION))
    extras.putLong(MediaItems.EXTRA_TRACK_NUMBER, getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER))
    extras.putLong(MediaItems.EXTRA_DISC_NUMBER, getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER))
    val artUri = getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)

    builder.setMediaId(MediaID.createMediaID(musicId, parentMediaId))
            .setTitle(getString(MediaMetadataCompat.METADATA_KEY_TITLE))
            .setSubtitle(getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
            .setMediaUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon()
                    .appendEncodedPath(musicId)
                    .build())
            .setExtras(extras)
    artUri?.let { builder.setIconUri(Uri.parse(it)) }

    return builder.build()
}
