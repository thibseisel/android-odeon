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
import fr.nihilus.music.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.media.MediaId.Builder.fromParts
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

/**
 * Define the hierarchy of media that can be browsed by remote clients connected to the service.
 * Media are organized in a tree-like structure, with 2 type of nodes:
 * - [Browsable items][MediaItem.isBrowsable] that have children themself that can be retrieved using [getChildren],
 * - [Playable leafs][MediaItem.isPlayable] that do not have children but can be played.
 */
internal interface BrowserTree {

    val updatedParentIds: Flowable<MediaId>

    /**
     * Retrieve children media of an item with the given [parentId] in the browser tree.
     * The nature of those children depends on the media id of its parent and the internal structure of the media tree.
     * See [MediaId] for more information.
     *
     * If the specified parent is browsable, this returns a list of items that may have children themselves ;
     * otherwise, if the parent is not browsable, `null` is returned to indicate the absence of children.
     * Likewise, if the specified media id does not match an existing media in the tree, this also returns `null`.
     *
     * @param parentId The media id of an item whose children should be loaded.
     * @return The list of children of the media with the id [parentId], or `null` if that media is not browsable
     * or doesn't exist.
     */
    suspend fun getChildren(parentId: MediaId): List<MediaItem>?

    /**
     * Retrieve an item identified by the specified [itemId] from the media tree.
     * If no item matches that media id, `null` is returned.
     *
     * @param itemId The media id of the item to retrieve.
     * @return An item with the same media id as the requested one, or `null` if no item matches.
     */
    suspend fun getItem(itemId: MediaId): MediaItem?

    suspend fun search(query: String, extras: Map<String, Any?>?): List<MediaItem>
}

private val ALBUM_TRACK_ORDERING = Comparator<Track> { a, b ->
    val discNumberDiff = a.discNumber - b.discNumber
    if (discNumberDiff != 0) discNumberDiff else (a.trackNumber - b.trackNumber)
}

internal class BrowserTreeImpl(
    private val repository: MediaRepository
) : BrowserTree {

    private val tree = mediaTree(MediaId.TYPE_ROOT) {

        type(MediaId.TYPE_TRACKS) {
            category(CATEGORY_ALL, title = "All Tracks") {
                val builder = MediaDescriptionCompat.Builder()
                repository.getAllTracks().map { track ->
                    val mediaId = MediaId.encode(type, CATEGORY_ALL, track.id.toString())
                    track.toMediaItem(mediaId, builder)
                }
            }

            category(CATEGORY_MOST_RATED, title = "Most Rated") {
                val builder = MediaDescriptionCompat.Builder()
                repository.getMostRatedTracks().map { track ->
                    val mediaId = MediaId.encode(type, CATEGORY_MOST_RATED, track.id.toString())
                    track.toMediaItem(mediaId, builder)
                }
            }

            category(CATEGORY_RECENTLY_ADDED, title = "Recently Added") {
                val builder = MediaDescriptionCompat.Builder()
                repository.getAllTracks()
                    .sortedByDescending { it.availabilityDate }
                    .take(25)
                    .map { track ->
                        val mediaId = MediaId.encode(type, CATEGORY_RECENTLY_ADDED, track.id.toString())
                        track.toMediaItem(mediaId, builder)
                    }
            }
        }

        type(MediaId.TYPE_ALBUMS) {
            categories {
                val builder = MediaDescriptionCompat.Builder()
                repository.getAllAlbums().map { album ->
                    val mediaId = MediaId.encode(type, album.id.toString())
                    album.toMediaItem(mediaId, builder)
                }
            }

            categoryChildren { albumCategory ->
                albumCategory.toLongOrNull()?.let { albumId ->
                    val builder = MediaDescriptionCompat.Builder()
                    repository.getAllTracks().asSequence()
                        .filter { it.albumId == albumId }
                        .sortedWith(ALBUM_TRACK_ORDERING)
                        .mapTo(mutableListOf()) { albumTrack ->
                            val mediaId = MediaId.encode(type, albumCategory, albumTrack.id.toString())
                            albumTrack.toMediaItem(mediaId, builder)
                    }.takeUnless { it.isEmpty() }
                }
            }
        }

        type(MediaId.TYPE_ARTISTS) {
            categories {
                val builder = MediaDescriptionCompat.Builder()
                repository.getAllArtists().map { artist ->
                    val mediaId = MediaId.encode(type, artist.id.toString())
                    artist.toMediaItem(mediaId, builder)
                }
            }

            categoryChildren { artistCategory ->
                artistCategory.toLongOrNull()?.let { artistId ->
                    loadArtistChildren(artistId)
                }
            }
        }

        type(MediaId.TYPE_PLAYLISTS) {
            categories {
                val builder = MediaDescriptionCompat.Builder()
                val allPlaylists = repository.getAllPlaylists()
                allPlaylists.map { playlist ->
                    val mediaId = MediaId.encode(type, playlist.id.toString())
                    playlist.toMediaItem(mediaId, builder)
                }
            }

            categoryChildren { playlistCategory ->
                playlistCategory.toLongOrNull()?.let { playlistId ->
                    val builder = MediaDescriptionCompat.Builder()
                    repository.getPlaylistTracks(playlistId)?.map { playlistTrack ->
                        val mediaId = MediaId.encode(type, playlistId.toString(), playlistTrack.id.toString())
                        playlistTrack.toMediaItem(mediaId, builder)
                    }
                }
            }
        }
    }

    private suspend fun loadArtistChildren(artistId: Long): List<MediaItem>? = coroutineScope {
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
                val mediaId = MediaId.encode(MediaId.TYPE_ALBUMS, it.id.toString())
                it.toMediaItem(mediaId, builder)
            }

            artistTracks.mapTo(artistChildren) {
                val mediaId = MediaId.encode(MediaId.TYPE_ARTISTS, artistId.toString(), it.id.toString())
                it.toMediaItem(mediaId, builder)
            }

        }.takeUnless { it.isEmpty() }
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

    override suspend fun getChildren(parentId: MediaId): List<MediaItem>? = tree.getChildren(parentId)

    override suspend fun getItem(itemId: MediaId): MediaItem? = tree.getItem(itemId)

    override suspend fun search(query: String, extras: Map<String, Any?>?): List<MediaItem> {
        // TODO Implement search.
        return emptyList()
    }

    override val updatedParentIds: Flowable<MediaId>
        get() = repository.changeNotifications.flatMap {
            if (it is ChangeNotification.AllTracks) Flowable.just(
                fromParts(TYPE_TRACKS, CATEGORY_ALL),
                fromParts(TYPE_TRACKS, CATEGORY_MOST_RATED),
                fromParts(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED)
            ) else Flowable.just(it.mediaId)
        }
}

