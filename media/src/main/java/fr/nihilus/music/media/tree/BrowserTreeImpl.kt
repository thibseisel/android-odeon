/*
 * Copyright 2019 Thibault Seisel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.nihilus.music.media.tree

import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.net.toUri
import fr.nihilus.music.media.MediaId
import fr.nihilus.music.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.media.MediaId.Builder.CATEGORY_MOST_RATED
import fr.nihilus.music.media.MediaId.Builder.CATEGORY_RECENTLY_ADDED
import fr.nihilus.music.media.MediaId.Builder.TYPE_ALBUMS
import fr.nihilus.music.media.MediaId.Builder.TYPE_ARTISTS
import fr.nihilus.music.media.MediaId.Builder.TYPE_PLAYLISTS
import fr.nihilus.music.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.media.MediaId.Builder.encode
import fr.nihilus.music.media.MediaItems
import fr.nihilus.music.media.R
import fr.nihilus.music.media.di.ServiceScoped
import fr.nihilus.music.media.playlists.Playlist
import fr.nihilus.music.media.provider.Album
import fr.nihilus.music.media.provider.Artist
import fr.nihilus.music.media.provider.Track
import fr.nihilus.music.media.repo.ChangeNotification
import fr.nihilus.music.media.repo.MediaRepository
import fr.nihilus.music.media.repo.mediaId
import io.reactivex.Flowable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

private const val DEFAULT_PAGE_NUMBER = 0
private const val DEFAULT_PAGE_SIZE = Int.MAX_VALUE
private const val MINIMUM_PAGE_NUMBER = 0
private const val MINIMUM_PAGE_SIZE = 1

private val ALBUM_TRACK_ORDERING = Comparator<Track> { a, b ->
    val discNumberDiff = a.discNumber - b.discNumber
    if (discNumberDiff != 0) discNumberDiff else (a.trackNumber - b.trackNumber)
}

@ServiceScoped
internal class BrowserTreeImpl
@Inject constructor(
    private val context: Context,
    private val repository: MediaRepository
) : BrowserTree {

    private val tree = mediaTree(MediaId.ROOT) {
        rootName = context.getString(R.string.browser_root_title)

        type(TYPE_TRACKS) {
            title = context.getString(R.string.tracks_type_title)

            category(
                CATEGORY_ALL,
                context.getString(R.string.abc_all_music),
                children = ::provideAllTracks
            )

            category(
                CATEGORY_MOST_RATED,
                context.getString(R.string.abc_most_rated),
                children = ::provideMostRatedTracks
            )

            category(
                CATEGORY_RECENTLY_ADDED,
                context.getString(R.string.abc_last_added),
                children = ::provideRecentlyAddedTracks
            )
        }

        type(TYPE_ALBUMS) {
            title = context.getString(R.string.albums_type_title)

            categories(provider = ::provideAllAlbums)
            categoryChildren(provider = ::provideAlbumTracks)
        }

        type(TYPE_ARTISTS) {
            title = context.getString(R.string.artists_type_title)

            categories(provider = ::provideAllArtists)
            categoryChildren(provider = ::provideArtistChildren)
        }

        type(TYPE_PLAYLISTS) {
            title = context.getString(R.string.playlists_type_title)

            categories(provider = ::provideAllPlaylists)
            categoryChildren(provider = ::providePlaylistTracks)
        }
    }

    override suspend fun getChildren(parentId: MediaId, options: Bundle?): List<MediaItem>? {
        // Take pagination into account when specified.
        val pageNumber = options?.getInt(MediaBrowserCompat.EXTRA_PAGE, DEFAULT_PAGE_NUMBER)
            ?.coerceAtLeast(MINIMUM_PAGE_NUMBER)
            ?: DEFAULT_PAGE_NUMBER

        val pageSize = options?.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, DEFAULT_PAGE_SIZE)
            ?.coerceAtLeast(MINIMUM_PAGE_SIZE)
            ?: DEFAULT_PAGE_SIZE

        return tree.getChildren(parentId, pageNumber, pageSize)
    }

    override suspend fun getItem(itemId: MediaId): MediaItem? = tree.getItem(itemId)

    override suspend fun search(query: String, extras: Bundle?): List<MediaItem> {
        val results = when (extras?.getString(MediaStore.EXTRA_MEDIA_FOCUS)) {
            MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE -> {
                extras.getString(MediaStore.EXTRA_MEDIA_ARTIST)?.let {
                    searchTracksByArtistName(it.toLowerCase())
                }
            }

            MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE -> {
                extras.getString(MediaStore.EXTRA_MEDIA_ALBUM)?.let {
                    searchTracksByAlbumTitle(it.toLowerCase())
                }
            }

            else -> searchTrackByTitle(query.toLowerCase())?.let(::listOf)
        }

        return results.orEmpty()
    }

    private suspend fun searchTracksByArtistName(artistName: String): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()

        return repository.getAllTracks().asSequence()
            .filter { artistName == it.artist.toLowerCase() }
            .map {
                val mediaId = encode(TYPE_ARTISTS, it.artistId.toString(), it.id)
                it.toMediaItem(mediaId, builder)
            }.toList()
    }

    private suspend fun searchTracksByAlbumTitle(albumTitle: String): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()

        return repository.getAllTracks().asSequence()
            .filter { albumTitle == it.album.toLowerCase() }
            .map {
                val mediaId = encode(TYPE_ALBUMS, it.albumId.toString(), it.id)
                it.toMediaItem(mediaId, builder)
            }.toList()
    }

    private suspend fun searchTrackByTitle(title: String?): MediaItem? {
        if (title == null) return null
        val builder = MediaDescriptionCompat.Builder()
        return repository.getAllTracks().find { title == it.title.toLowerCase() }?.let {
            val mediaId = encode(TYPE_TRACKS, CATEGORY_ALL, it.id)
            it.toMediaItem(mediaId, builder)
        }
    }

    override val updatedParentIds: Flowable<MediaId>
        get() = repository.changeNotifications.flatMap {
            if (it is ChangeNotification.AllTracks) Flowable.just(
                MediaId.fromParts(TYPE_TRACKS, CATEGORY_ALL),
                MediaId.fromParts(TYPE_TRACKS, CATEGORY_MOST_RATED),
                MediaId.fromParts(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED)
            ) else Flowable.just(it.mediaId)
        }

    private suspend fun provideAllTracks(fromIndex: Int, count: Int): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()

        return repository.getAllTracks().asSequence()
            .drop(fromIndex)
            .take(count)
            .mapTo(mutableListOf()) { track ->
                val mediaId = encode(TYPE_TRACKS, CATEGORY_ALL, track.id)
                track.toMediaItem(mediaId, builder)
            }
    }

    private suspend fun provideMostRatedTracks(fromIndex: Int, count: Int): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()

        return repository.getMostRatedTracks().asSequence()
            .drop(fromIndex)
            .take(count)
            .mapTo(mutableListOf()) { track ->
                val mediaId = encode(TYPE_TRACKS, CATEGORY_MOST_RATED, track.id)
                track.toMediaItem(mediaId, builder)
            }
    }

    private suspend fun provideRecentlyAddedTracks(fromIndex: Int, count: Int): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()

        return repository.getAllTracks().asSequence()
            .sortedByDescending { it.availabilityDate }
            .take(25)
            .drop(fromIndex)
            .take(count)
            .mapTo(mutableListOf()) { track ->
                val mediaId = encode(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, track.id)
                track.toMediaItem(mediaId, builder)
            }
    }

    private suspend fun provideAllAlbums(): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()

        return repository.getAllAlbums().map { album ->
            val mediaId = encode(TYPE_ALBUMS, album.id.toString())
            album.toMediaItem(mediaId, builder)
        }
    }

    private suspend fun provideAlbumTracks(
        albumCategory: String,
        fromIndex: Int,
        count: Int
    ): List<MediaItem>? {
        return albumCategory.toLongOrNull()?.let { albumId ->
            val builder = MediaDescriptionCompat.Builder()

            repository.getAllTracks().asSequence()
                .filter { it.albumId == albumId }
                .sortedWith(ALBUM_TRACK_ORDERING)
                .drop(fromIndex)
                .take(count)
                .mapTo(mutableListOf()) { albumTrack ->
                    val mediaId = encode(TYPE_ALBUMS, albumCategory, albumTrack.id)
                    albumTrack.toMediaItem(mediaId, builder)
                }
                .takeUnless { it.isEmpty() }
        }
    }

    private suspend fun provideAllArtists(): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()

        return repository.getAllArtists().map { artist ->
            val mediaId = encode(TYPE_ARTISTS, artist.id.toString())
            artist.toMediaItem(mediaId, builder)
        }
    }

    private suspend fun provideArtistChildren(
        artistCategory: String,
        fromIndex: Int,
        count: Int
    ): List<MediaItem>? {
        return artistCategory.toLongOrNull()?.let { artistId ->
            coroutineScope {
                val builder = MediaDescriptionCompat.Builder()

                val asyncAllAlbums = async { repository.getAllAlbums() }
                val asyncAllTracks = async { repository.getAllTracks() }

                val artistAlbums = asyncAllAlbums.await().asSequence()
                    .filter { it.artistId == artistId }
                    .sortedByDescending { it.releaseYear }
                    .map { album ->
                        val mediaId = encode(TYPE_ALBUMS, album.id.toString())
                        album.toMediaItem(mediaId, builder)
                    }

                val artistTracks = asyncAllTracks.await().asSequence()
                    .filter { it.artistId == artistId }
                    .map { track ->
                        val mediaId = encode(TYPE_ARTISTS, artistId.toString(), track.id)
                        track.toMediaItem(mediaId, builder)
                    }

                (artistAlbums + artistTracks)
                    .drop(fromIndex)
                    .take(count)
                    .toList()
                    .takeUnless { it.isEmpty() }
            }
        }
    }

    private suspend fun provideAllPlaylists(): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()
        val allPlaylists = repository.getAllPlaylists()

        return allPlaylists.map { playlist ->
            val mediaId = encode(TYPE_PLAYLISTS, playlist.id.toString())
            playlist.toMediaItem(mediaId, builder)
        }
    }

    private suspend fun providePlaylistTracks(
        playlistCategory: String,
        fromIndex: Int,
        count: Int
    ): List<MediaItem>? {
        return playlistCategory.toLongOrNull()?.let { playlistId ->
            val builder = MediaDescriptionCompat.Builder()

            repository.getPlaylistTracks(playlistId)?.asSequence()
                ?.drop(fromIndex)
                ?.take(count)
                ?.mapTo(mutableListOf()) { playlistTrack ->
                    val mediaId = encode(TYPE_PLAYLISTS, playlistId.toString(), playlistTrack.id)
                    playlistTrack.toMediaItem(mediaId, builder)
                }
        }
    }

    private fun Track.toMediaItem(mediaId: String, builder: MediaDescriptionCompat.Builder): MediaItem {
        val description = builder.setMediaId(mediaId)
            .setTitle(title)
            .setSubtitle(artist)
            .setMediaUri(mediaUri.toUri())
            .setIconUri(albumArtUri?.toUri())
            .setExtras(Bundle().apply {
                putLong(MediaItems.EXTRA_DURATION, duration)
                putInt(MediaItems.EXTRA_DISC_NUMBER, discNumber)
                putInt(MediaItems.EXTRA_TRACK_NUMBER, trackNumber)
            }).build()

        return MediaItem(description, MediaItem.FLAG_PLAYABLE)
    }

    private fun Album.toMediaItem(mediaId: String, builder: MediaDescriptionCompat.Builder): MediaItem {
        val albumDescription = builder.setMediaId(mediaId)
            .setTitle(title)
            .setSubtitle(artist)
            .setIconUri(albumArtUri?.toUri())
            .setExtras(Bundle().apply {
                putInt(MediaItems.EXTRA_NUMBER_OF_TRACKS, trackCount)
            })
            .build()
        return MediaItem(albumDescription, MediaItem.FLAG_BROWSABLE)
    }

    private fun Artist.toMediaItem(mediaId: String, builder: MediaDescriptionCompat.Builder): MediaItem {
        val artistDescription = builder.setMediaId(mediaId)
            .setTitle(name)
            .setIconUri(iconUri?.toUri())
            .setExtras(Bundle().apply {
                putInt(MediaItems.EXTRA_NUMBER_OF_TRACKS, trackCount)
            })
            .build()
        return MediaItem(artistDescription, MediaItem.FLAG_BROWSABLE)
    }

    private fun Playlist.toMediaItem(mediaId: String, builder: MediaDescriptionCompat.Builder): MediaItem {
        val playlistDescription = builder.setMediaId(mediaId)
            .setTitle(title)
            .setIconUri(iconUri)
            .build()
        return MediaItem(playlistDescription, MediaItem.FLAG_BROWSABLE)
    }
}