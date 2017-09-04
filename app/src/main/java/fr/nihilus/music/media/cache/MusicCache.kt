package fr.nihilus.music.media.cache

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat

interface MusicCache {

    /**
     * Put media items in the cache. Those items could then be retrieved with [getItems].
     * Depending on the cache implementation, older items may be removed
     * as other set of media items are put into the cache.
     * @param mediaId unique identifier of the parent that contains the media items
     */
    fun putItems(mediaId: String, items: List<MediaBrowserCompat.MediaItem>)

    /**
     * Retrieve a set of media items stored in the cache.
     * Depending on the cache implementation, the requested items may or may not be in the cache,
     * even if it has been saved in [putItems] earlier.
     * In case items are absent, an empty list will be returned.
     *
     * @param mediaId of the parent that contains the media items
     * @return a list of those media items, or an empty list if not in cache
     */
    fun getItems(mediaId: String): List<MediaBrowserCompat.MediaItem>

    /**
     * Put a metadata in the cache.
     * @param musicId the id the metadata to be stored
     * @param metadata the metadata to store, whose id is [musicId]
     */
    fun putMetadata(musicId: String, metadata: MediaMetadataCompat)

    /**
     * Retrieve a metadata for the given [musicId] from the cache.
     * Depending on the cache implementation, the requested item may or may not be in the cache
     * at a later time ; if it has been removed or if it has never been in the cache, the returned
     * metadata will be null.
     * @param musicId the id of the requested metadata
     */
    fun getMetadata(musicId: String): MediaMetadataCompat?

    /**
     * Remove all items stored in the cache, effectively releasing all reference to them.
     */
    fun clear()
}

