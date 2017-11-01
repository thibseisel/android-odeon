package fr.nihilus.music.media.repo

import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaMetadataCompat
import fr.nihilus.music.media.builtin.BuiltinItem
import fr.nihilus.music.media.cache.MusicCache
import fr.nihilus.music.media.source.MusicDao
import fr.nihilus.music.utils.MediaID
import io.reactivex.Maybe
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A Music Repository that tries to fetch items and metadata from cache,
 * and then from the data source if not available.
 */
@Singleton
internal class CachedMusicRepository
@Inject constructor(
        private val mediaDao: MusicDao,
        private val musicCache: MusicCache,
        private val builtIns: Map<String, @JvmSuppressWildcards BuiltinItem>
) : MusicRepository {

    override fun getMediaItems(parentMediaId: String): Single<List<MediaItem>> {
        // Get the "true" parent in case the passed media id is a playable item
        val trueParent = MediaID.stripMusicId(parentMediaId)

        val cachedItems = musicCache.getItems(trueParent)
        if (cachedItems.isNotEmpty()) {
            return Single.just(cachedItems)
        }

        val parentHierarchy = MediaID.getHierarchy(trueParent)
        // Search the root media id in built-in items
        // Notify an error if no built-in is found
        val builtIn = builtIns[parentHierarchy[0]]
                ?: return Single.error(::UnsupportedOperationException)
        val items = builtIn.getChildren(trueParent)

        return items.doOnSuccess { musicCache.putItems(trueParent, it) }
    }

    override fun getMetadata(musicId: String): Single<MediaMetadataCompat> {
        return Maybe.fromCallable<MediaMetadataCompat> { musicCache.getMetadata(musicId) }
                .concatWith(mediaDao.getTrack(musicId))
                .firstOrError()
    }

    override fun clear() = musicCache.clear()
}
