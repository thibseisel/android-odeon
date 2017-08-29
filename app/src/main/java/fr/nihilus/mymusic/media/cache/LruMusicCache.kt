package fr.nihilus.mymusic.media.cache

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.util.ArrayMap
import android.support.v4.util.LruCache
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LruMusicCache"

/**
 * A MusicCache implementation that keeps only the latest media items in memory,
 * but keep all metadata.
 */
@Singleton
class LruMusicCache
@Inject constructor() : MusicCache {
    private val mItemsCache = LruCache<String, List<MediaBrowserCompat.MediaItem>>(5)
    private val mMetadataCache = ArrayMap<String, MediaMetadataCompat>()

    override fun putItems(mediaId: String, items: List<MediaBrowserCompat.MediaItem>) {
        Log.d(TAG, "Putting items: ($mediaId) -> size=${items.size}")
        mItemsCache.put(mediaId, items)
    }

    override fun getItems(mediaId: String): List<MediaBrowserCompat.MediaItem> {
        val items = mItemsCache.get(mediaId) ?: emptyList()
        Log.d(TAG, "Retrieving items: ($mediaId) -> size=${items.size}")
        return items
    }

    override fun putMetadata(musicId: String, metadata: MediaMetadataCompat) {
        mMetadataCache.put(musicId, metadata)
    }

    override fun getMetadata(musicId: String): MediaMetadataCompat? {
        val metadata = mMetadataCache[musicId]
        Log.d(TAG, "Retrieved from cache: ($musicId) -> $metadata")
        return metadata
    }

    override fun clear() {
        mMetadataCache.clear()
        mItemsCache.evictAll()
    }
}