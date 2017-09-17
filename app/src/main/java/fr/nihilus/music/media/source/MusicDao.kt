package fr.nihilus.music.media.source

import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import fr.nihilus.music.media.MediaItems
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable

/**
 * Gives access to music items stored on a given datastore.
 *
 * Because retrieving data can be long running especially from the network,
 * this API use ReactiveX `Observable` classes to represent deferred computations:
 * if there's no item corresponding to the request, implementations should emit an empty list
 * (or no item for operations that returns only one).
 * Furthermore, if an operation is not supported, then the returned `Observable`
 * should emit an error notification with an [UnsupportedOperationException].
 */
interface MusicDao {

    /**
     * Retrieve all tracks from a given datastore.
     * See [getTrack] for a list of metadata properties that implementations
     * should define for each track.
     * @return an observable that emits a list of all tracks
     */
    fun getAllTracks(): Observable<List<MediaMetadataCompat>>

    /**
     * Retrieve a single track's metadata from a given datastore.
     * The metadata properties may vary depending on the implementation.
     *
     * Implementations **must** define the following properties for each track:
     * - [MediaMetadataCompat.METADATA_KEY_MEDIA_ID]
     * - [MediaMetadataCompat.METADATA_KEY_TITLE]
     * - [MediaMetadataCompat.METADATA_KEY_ALBUM]
     * - [MediaMetadataCompat.METADATA_KEY_ARTIST]
     * - [MediaMetadataCompat.METADATA_KEY_DURATION]
     * - [MediaMetadataCompat.METADATA_KEY_DISC_NUMBER]
     * - [MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER]
     * - [MediaMetadataCompat.METADATA_KEY_MEDIA_URI]
     *
     * The following properties are optionnal:
     * - [MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI]
     * - [MusicDao.CUSTOM_META_TITLE_KEY]
     * - [MusicDao.CUSTOM_META_ALBUM_ID]
     * - [MusicDao.CUSTOM_META_ARTIST_ID]
     *
     * The returned observable may complete without returning a value, indicating that the
     * requested track does not exist on this datastore.
     * @param musicId the unique numeric identifier of the track to retrieve
     * @return an observable that emits the requested item
     * or completes without emitting if it does not exist.
     */
    fun getTrack(musicId: String): Maybe<MediaMetadataCompat>

    /**
     * Return an observable dataset of albums featuring music stored on this device.
     *
     * Each album is composed of :
     * - a media id
     * - a title
     * - a subtitle, which is the name of the artist that composed it
     * - a content URI pointing to the album art
     * - the year at which it was released ([MediaItems.EXTRA_YEAR])
     * - the number of songs it featured ([MediaItems.EXTRA_NUMBER_OF_TRACKS])
     * - a key used for alphabetic sorting ([MediaItems.EXTRA_TITLE_KEY]).
     *
     * Albums are sorted by name by default.
     */
    fun getAlbums(): Observable<List<MediaDescriptionCompat>>

    /**
     * Return an observable dataset of artists that participated to composing
     * music stored on this device.
     *
     * Each artist is composed of :
     * - a media id
     * - its name
     * - a content URI pointing to the album art of the most recent album
     * - the number of songs it composed ([MediaItems.EXTRA_NUMBER_OF_TRACKS])
     * - a key used for alphabeting sorting ([MediaItems.EXTRA_TITLE_KEY]).
     *
     * Artists are sorted by name by default.
     */
    fun getArtists(): Observable<List<MediaDescriptionCompat>>

    /**
     * Retrieve tracks that are part of a given album.
     * @param albumId unique identifier of the album
     * @return track metadatas from this album sorted by track number
     */
    fun getAlbumTracks(albumId: String): Observable<List<MediaMetadataCompat>>

    /**
     * Retrieve tracks that are produced by a given artist.
     * @param artistId unique identifier of the artist
     * @return track metadatas from this artist sorted by track name
     */
    fun getArtistTracks(artistId: String): Observable<List<MediaMetadataCompat>>

    /**
     * Retrieve albums that are produced by a given artist.
     * @param artistId unique identifier of the artist
     * @return informations of albums from this artist sorted by descending release date
     */
    fun getArtistAlbums(artistId: String): Observable<List<MediaDescriptionCompat>>

    /**
     * Delete the track with the specified [trackId] from a given datastore.
     * If no track exist with this id, the operation will terminate without an error.
     * @return the task to execute
     */
    fun deleteTrack(trackId: String): Completable

    companion object {
        const val CUSTOM_META_TITLE_KEY = "title_key"
        const val CUSTOM_META_ALBUM_ID = "album_id"
        const val CUSTOM_META_ARTIST_ID = "artist_id"
    }
}