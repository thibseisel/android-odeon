package fr.nihilus.music.media.repo

import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import fr.nihilus.music.asMediaDescription
import fr.nihilus.music.media.cache.MusicCache
import fr.nihilus.music.media.source.MusicDao
import fr.nihilus.music.utils.MediaID
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A Music Repository that tries to fetch items and metadata from cache,
 * and then from the datasource if not available.
 */
@Singleton
internal class CachedMusicRepository
@Inject constructor(mediaDao: MusicDao, musicCache: MusicCache) : MusicRepository {

    private val mDao = mediaDao
    private val mCache = musicCache
    private val metadatas = mediaDao.getAllTracks()
            .doOnNext(this::cacheMetadatas)
            .share()

    override fun getMediaItems(parentMediaId: String): Single<List<MediaItem>> {
        // Get the "true" parent in case the passed media id is a playable item
        val trueParent = MediaID.stripMusicId(parentMediaId)

        val cachedItems = mCache.getItems(trueParent)
        if (cachedItems.isNotEmpty()) {
            return Single.just(cachedItems)
        }

        val parentHierarchy = MediaID.getHierarchy(trueParent)
        val items = when (parentHierarchy[0]) {
            MediaID.ID_MUSIC -> getAllTracks()
            MediaID.ID_ALBUMS -> {
                if (parentHierarchy.size > 1) getAlbumChildren(parentHierarchy[1])
                else getAlbums()
            }
            MediaID.ID_ARTISTS -> {
                if (parentHierarchy.size > 1) getArtistChildren(parentHierarchy[1])
                else getArtists()
            }
            else -> Single.error(::UnsupportedOperationException)
        }

        return items.doOnSuccess { mCache.putItems(trueParent, it) }
    }

    private fun getAllTracks(): Single<List<MediaItem>> {
        val builder = MediaDescriptionCompat.Builder()
        return metadatas.single(emptyList()).toObservable()
                .flatMap { Observable.fromIterable(it) }
                .map { it.asMediaDescription(builder, MediaID.ID_MUSIC) }
                .map { MediaItem(it, MediaItem.FLAG_PLAYABLE) }
                .toList()
    }

    private fun getAlbums(): Single<List<MediaItem>> {
        return mDao.getAlbums().flatMap { Observable.fromIterable(it) }
                .map { MediaItem(it, MediaItem.FLAG_BROWSABLE or MediaItem.FLAG_PLAYABLE) }
                .toList()
    }

    private fun getAlbumChildren(albumId: String): Single<List<MediaItem>> {
        val builder = MediaDescriptionCompat.Builder()
        return mDao.getAlbumTracks(albumId)
                .flatMap { Observable.fromIterable(it) }
                .map { it.asMediaDescription(builder, MediaID.ID_ALBUMS, albumId) }
                .map { MediaItem(it, MediaItem.FLAG_PLAYABLE) }
                .toList()
    }

    private fun getArtistChildren(artistId: String): Single<List<MediaItem>> {
        val builder = MediaDescriptionCompat.Builder()
        val albums = mDao.getArtistAlbums(artistId)
                .flatMap { Observable.fromIterable(it) }
                .map { MediaItem(it, MediaItem.FLAG_BROWSABLE or MediaItem.FLAG_BROWSABLE) }
        val tracks = mDao.getArtistTracks(artistId)
                .flatMap { Observable.fromIterable(it) }
                .map { it.asMediaDescription(builder, MediaID.ID_ARTISTS, artistId) }
                .map { MediaItem(it, MediaItem.FLAG_PLAYABLE) }
        return Observable.concat(albums, tracks).toList()
    }

    private fun getArtists(): Single<List<MediaItem>> {
        return mDao.getArtists().flatMap { Observable.fromIterable(it) }
                .map { MediaItem(it, MediaItem.FLAG_BROWSABLE or MediaItem.FLAG_PLAYABLE) }
                .toList()
    }

    override fun getMetadata(musicId: String): Single<MediaMetadataCompat> {
        val fromCache: Maybe<MediaMetadataCompat> = Maybe.fromCallable { mCache.getMetadata(musicId) }
        return fromCache.concatWith(mDao.getTrack(musicId)).firstOrError()
    }

    /**
     * An Observable that notify observers when a change is detected in the music library.
     * It emits the parent media id of the collection of items that have change.
     *
     * Clients are then advised to call [getMediaItems] to fetch up-to-date items.
     *
     * When done observing changes, clients __must__ dispose their observers
     * through [Disposable.dispose] to avoid memory leaks.
     */
    val mediaChanges: Observable<String> by lazy {
        // TODO Add any other media that are subject to changes
        metadatas.map { _ -> MediaID.ID_MUSIC }
    }

    override fun clear() {
        mCache.clear()
    }

    private fun cacheMetadatas(metadataList: List<MediaMetadataCompat>) {
        for (meta in metadataList) {
            val musicId = meta.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
            mCache.putMetadata(musicId, meta)
        }
    }
}
