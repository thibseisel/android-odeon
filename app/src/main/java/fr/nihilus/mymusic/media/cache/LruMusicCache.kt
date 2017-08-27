package fr.nihilus.mymusic.media.cache

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.util.ArrayMap
import android.support.v4.util.LruCache
import javax.inject.Inject
import javax.inject.Singleton

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
        mItemsCache.put(mediaId, items)
    }

    override fun getItems(mediaId: String) = mItemsCache.get(mediaId) ?: emptyList()

    override fun putMetadata(musicId: String, metadata: MediaMetadataCompat) {
        mMetadataCache.put(musicId, metadata)
    }

    override fun getMetadata(musicId: String) = mMetadataCache[musicId]

    override fun clear() {
        mMetadataCache.clear()
        mItemsCache.evictAll()
    }
}