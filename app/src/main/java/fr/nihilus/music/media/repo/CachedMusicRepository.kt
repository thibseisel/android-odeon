package fr.nihilus.music.media.repo

import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import fr.nihilus.music.asMediaDescription
import fr.nihilus.music.database.PlaylistDao
import fr.nihilus.music.media.builtin.BuiltinItem
import fr.nihilus.music.media.cache.MusicCache
import fr.nihilus.music.media.source.MusicDao
import fr.nihilus.music.utils.MediaID
import io.reactivex.Flowable
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
@Inject constructor(
        private val mediaDao: MusicDao,
        private val musicCache: MusicCache,
        private val playlistDao: PlaylistDao,
        private val builtIns: Map<String, @JvmSuppressWildcards BuiltinItem>
) : MusicRepository {

    private val metadatas = mediaDao.getAllTracks()
            .doOnNext(this::cacheMetadatas)
            .share()

    override fun getMediaItems(parentMediaId: String): Single<List<MediaItem>> {
        // Get the "true" parent in case the passed media id is a playable item
        val trueParent = MediaID.stripMusicId(parentMediaId)

        val cachedItems = musicCache.getItems(trueParent)
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
            MediaID.ID_PLAYLISTS -> {
                if (parentHierarchy.size > 1) getPlaylistMembers(parentHierarchy[1])
                else getPlaylists()
            }
            else -> {
                // Search the root media id in built-in items
                // Notify an error if no built-in is found
                val builtIn = builtIns.get(parentHierarchy[0])
                        ?: return Single.error(::UnsupportedOperationException)
                return builtIn.getChildren()
            }
        }

        return items.doOnSuccess { musicCache.putItems(trueParent, it) }
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
        return mediaDao.getAlbums().flatMap { Observable.fromIterable(it) }
                .map { MediaItem(it, MediaItem.FLAG_BROWSABLE or MediaItem.FLAG_PLAYABLE) }
                .toList()
    }

    private fun getAlbumChildren(albumId: String): Single<List<MediaItem>> {
        val builder = MediaDescriptionCompat.Builder()
        return mediaDao.getAlbumTracks(albumId)
                .flatMap { Observable.fromIterable(it) }
                .map { it.asMediaDescription(builder, MediaID.ID_ALBUMS, albumId) }
                .map { MediaItem(it, MediaItem.FLAG_PLAYABLE) }
                .toList()
    }

    private fun getArtistChildren(artistId: String): Single<List<MediaItem>> {
        val builder = MediaDescriptionCompat.Builder()
        val albums = mediaDao.getArtistAlbums(artistId)
                .flatMap { Observable.fromIterable(it) }
                .map { MediaItem(it, MediaItem.FLAG_BROWSABLE or MediaItem.FLAG_BROWSABLE) }
        val tracks = mediaDao.getArtistTracks(artistId)
                .flatMap { Observable.fromIterable(it) }
                .map { it.asMediaDescription(builder, MediaID.ID_ARTISTS, artistId) }
                .map { MediaItem(it, MediaItem.FLAG_PLAYABLE) }
        return Observable.concat(albums, tracks).toList()
    }

    private fun getArtists(): Single<List<MediaItem>> {
        return mediaDao.getArtists().flatMap { Observable.fromIterable(it) }
                .map { MediaItem(it, MediaItem.FLAG_BROWSABLE or MediaItem.FLAG_PLAYABLE) }
                .toList()
    }

    private fun getPlaylists(): Single<List<MediaItem>> {
        val builder = MediaDescriptionCompat.Builder()
        return playlistDao.playlists.take(1)
                .flatMap { Flowable.fromIterable(it) }
                .map { playlist ->
                    val description = playlist.asMediaDescription(builder)
                    MediaItem(description, MediaItem.FLAG_BROWSABLE or MediaItem.FLAG_PLAYABLE)
                }.toList()
    }

    private fun getPlaylistMembers(playlistId: String): Single<List<MediaItem>> {
        val builder = MediaDescriptionCompat.Builder()
        return playlistDao.getPlaylistTracks(playlistId.toLong()).take(1)
                .flatMap { Flowable.fromIterable(it) }
                .flatMapSingle { getMetadata(it.musicId.toString()) }
                .map { member ->
                    val descr = member.asMediaDescription(builder, MediaID.ID_PLAYLISTS, playlistId)
                    MediaItem(descr, MediaItem.FLAG_PLAYABLE)
                }
                .toList()
    }

    override fun getMetadata(musicId: String): Single<MediaMetadataCompat> {
        val fromusicCache: Maybe<MediaMetadataCompat> = Maybe.fromCallable { musicCache.getMetadata(musicId) }
        return fromusicCache.concatWith(mediaDao.getTrack(musicId)).firstOrError()
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

    override fun clear() = musicCache.clear()

    private fun cacheMetadatas(metadataList: List<MediaMetadataCompat>) {
        for (meta in metadataList) {
            val musicId = meta.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
            musicCache.putMetadata(musicId, meta)
        }
    }
}
