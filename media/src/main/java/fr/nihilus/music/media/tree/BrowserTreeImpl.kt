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

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import fr.nihilus.music.media.MediaId
import fr.nihilus.music.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.media.MediaId.Builder.CATEGORY_MOST_RATED
import fr.nihilus.music.media.MediaId.Builder.CATEGORY_RECENTLY_ADDED
import fr.nihilus.music.media.MediaId.Builder.TYPE_ALBUMS
import fr.nihilus.music.media.MediaId.Builder.TYPE_ARTISTS
import fr.nihilus.music.media.MediaId.Builder.TYPE_PLAYLISTS
import fr.nihilus.music.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.media.MediaItems
import fr.nihilus.music.media.playlists.Playlist
import fr.nihilus.music.media.provider.Album
import fr.nihilus.music.media.provider.Artist
import fr.nihilus.music.media.provider.Track
import fr.nihilus.music.media.repo.ChangeNotification
import fr.nihilus.music.media.repo.MediaRepository
import fr.nihilus.music.media.repo.mediaId
import fr.nihilus.music.media.toUri
import io.reactivex.Flowable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private val ALBUM_TRACK_ORDERING = Comparator<Track> { a, b ->
    val discNumberDiff = a.discNumber - b.discNumber
    if (discNumberDiff != 0) discNumberDiff else (a.trackNumber - b.trackNumber)
}

internal class BrowserTreeImpl(
    private val repository: MediaRepository
) : BrowserTree {

    private val tree = mediaTree(MediaId.TYPE_ROOT) {

        type(TYPE_TRACKS) {
            category(CATEGORY_ALL, "All Tracks", children = ::provideAllTracks)
            category(CATEGORY_MOST_RATED, "Most Rated", children = ::provideMostRatedTracks)
            category(CATEGORY_RECENTLY_ADDED, "Recently Added", children = ::provideRecentlyAddedTracks)
        }

        type(TYPE_ALBUMS) {
            categories(provider = ::provideAllAlbums)
            categoryChildren(provider = ::provideAlbumTracks)
        }

        type(TYPE_ARTISTS) {
            categories(provider = ::provideAllArtists)
            categoryChildren(provider = ::provideArtistChildren)
        }

        type(TYPE_PLAYLISTS) {
            categories(provider = ::provideAllPlaylists)
            categoryChildren(provider = ::providePlaylistTracks)
        }
    }

    override suspend fun getChildren(parentId: MediaId): List<MediaItem>? = tree.getChildren(parentId)

    override suspend fun getItem(itemId: MediaId): MediaItem? = tree.getItem(itemId)

    override suspend fun search(query: String, extras: Map<String, Any?>?): List<MediaItem> {
        // TODO Implement search.
        return emptyList()
    }

    override val updatedParentIds: Flowable<MediaId>
        get() = repository.changeNotifications.flatMap {
            if (it is ChangeNotification.AllTracks) Flowable.just(
                MediaId.fromParts(TYPE_TRACKS, CATEGORY_ALL),
                MediaId.fromParts(TYPE_TRACKS, CATEGORY_MOST_RATED),
                MediaId.fromParts(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED)
            ) else Flowable.just(it.mediaId)
        }

    private suspend fun provideAllTracks(): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()

        return repository.getAllTracks().map { track ->
            val mediaId = MediaId.encode(
                TYPE_TRACKS,
                CATEGORY_ALL,
                track.id
            )
            track.toMediaItem(mediaId, builder)
        }
    }

    private suspend fun provideMostRatedTracks(): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()

        return repository.getMostRatedTracks().map { track ->
            val mediaId = MediaId.encode(TYPE_TRACKS, CATEGORY_MOST_RATED, track.id)
            track.toMediaItem(mediaId, builder)
        }
    }

    private suspend fun provideRecentlyAddedTracks(): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()

        return repository.getAllTracks()
            .sortedByDescending { it.availabilityDate }
            .take(25)
            .map { track ->
                val mediaId = MediaId.encode(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, track.id)
                track.toMediaItem(mediaId, builder)
            }
    }

    private suspend fun provideAllAlbums(): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()

        return repository.getAllAlbums().map { album ->
            val mediaId = MediaId.encode(TYPE_ALBUMS, album.id.toString())
            album.toMediaItem(mediaId, builder)
        }
    }

    private suspend fun provideAlbumTracks(albumCategory: String): List<MediaItem>? {
        return albumCategory.toLongOrNull()?.let { albumId ->
            val builder = MediaDescriptionCompat.Builder()

            repository.getAllTracks().asSequence()
                .filter { it.albumId == albumId }
                .sortedWith(ALBUM_TRACK_ORDERING)
                .mapTo(mutableListOf()) { albumTrack ->
                    val mediaId = MediaId.encode(TYPE_ALBUMS, albumCategory, albumTrack.id)
                    albumTrack.toMediaItem(mediaId, builder)
                }.takeUnless { it.isEmpty() }
        }
    }

    private suspend fun provideAllArtists(): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()

        return repository.getAllArtists().map { artist ->
            val mediaId = MediaId.encode(TYPE_ARTISTS, artist.id.toString())
            artist.toMediaItem(mediaId, builder)
        }
    }

    private suspend fun provideArtistChildren(artistCategory: String): List<MediaItem>? {
        return artistCategory.toLongOrNull()?.let { artistId ->
            coroutineScope {
                val asyncAllAlbums = async { repository.getAllAlbums() }
                val asyncAllTracks = async { repository.getAllTracks() }

                val artistAlbums = asyncAllAlbums.await().asSequence()
                    .filter { it.artistId == artistId }
                    .sortedByDescending { it.releaseYear }

                val artistTracks = asyncAllTracks.await().asSequence()
                    .filter { it.artistId == artistId }

                val builder = MediaDescriptionCompat.Builder()
                mutableListOf<MediaItem>().also { artistChildren ->
                    artistAlbums.mapTo(artistChildren) {
                        val mediaId = MediaId.encode(TYPE_ALBUMS, it.id.toString())
                        it.toMediaItem(mediaId, builder)
                    }

                    artistTracks.mapTo(artistChildren) {
                        val mediaId = MediaId.encode(TYPE_ARTISTS, artistId.toString(), it.id)
                        it.toMediaItem(mediaId, builder)
                    }

                }.takeUnless { it.isEmpty() }
            }
        }
    }

    private suspend fun provideAllPlaylists(): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()
        val allPlaylists = repository.getAllPlaylists()

        return allPlaylists.map { playlist ->
            val mediaId = MediaId.encode(TYPE_PLAYLISTS, playlist.id.toString())
            playlist.toMediaItem(mediaId, builder)
        }
    }

    private suspend fun providePlaylistTracks(playlistCategory: String): List<MediaItem>? {
        return playlistCategory.toLongOrNull()?.let { playlistId ->
            val builder = MediaDescriptionCompat.Builder()

            repository.getPlaylistTracks(playlistId)?.map { playlistTrack ->
                val mediaId = MediaId.encode(TYPE_PLAYLISTS, playlistId.toString(), playlistTrack.id)
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