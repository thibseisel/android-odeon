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
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.net.toUri
import fr.nihilus.music.media.MediaId
import fr.nihilus.music.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.media.MediaId.Builder.CATEGORY_DISPOSABLE
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
import fr.nihilus.music.media.extensions.getResourceUri
import fr.nihilus.music.media.playlists.Playlist
import fr.nihilus.music.media.provider.Album
import fr.nihilus.music.media.provider.Artist
import fr.nihilus.music.media.provider.Track
import fr.nihilus.music.media.repo.ChangeNotification
import fr.nihilus.music.media.repo.MediaRepository
import fr.nihilus.music.media.repo.mediaId
import fr.nihilus.music.media.service.SearchQuery
import fr.nihilus.music.media.usage.MediaUsageManager
import io.reactivex.Flowable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * The default value of the [MediaBrowserCompat.EXTRA_PAGE] option of [BrowserTree.getChildren]
 * when none is specified. This is the index of the first page.
 */
private const val DEFAULT_PAGE_NUMBER = 0

/**
 * The default value of the [MediaBrowserCompat.EXTRA_PAGE_SIZE] option of [BrowserTree.getChildren]
 * when none is specified. All children will be returned in the same page.
 */
private const val DEFAULT_PAGE_SIZE = Int.MAX_VALUE

/**
 * The minimum accepted value for the [MediaBrowserCompat.EXTRA_PAGE] option.
 * This is the index of the first page.
 */
private const val MINIMUM_PAGE_NUMBER = 0

/**
 * The minimum accepted value for the [MediaBrowserCompat.EXTRA_PAGE_SIZE] option.
 * This is the minimum of items that can be displayed in a page.
 */
private const val MINIMUM_PAGE_SIZE = 1

/**
 * The arbitrary maximum correspondence score for a fuzzy match.
 * When a string matches a fuzzy pattern, it is initially given this score.
 */
private const val BASE_SCORE = 100

private val ALBUM_TRACK_ORDERING = Comparator<Track> { a, b ->
    val discNumberDiff = a.discNumber - b.discNumber
    if (discNumberDiff != 0) discNumberDiff else (a.trackNumber - b.trackNumber)
}

@ServiceScoped
internal class BrowserTreeImpl
@Inject constructor(
    private val context: Context,
    private val repository: MediaRepository,
    private val usageManager: MediaUsageManager
) : BrowserTree {

    /**
     * The tree structure of the media browser.
     */
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
                iconUri = context.resources.getResourceUri(R.drawable.abc_ic_most_rated_128dp),
                children = ::provideMostRatedTracks
            )

            category(
                CATEGORY_RECENTLY_ADDED,
                context.getString(R.string.abc_last_added),
                iconUri = context.resources.getResourceUri(R.drawable.abc_ic_most_recent_128dp),
                children = ::provideRecentlyAddedTracks
            )

            category(
                CATEGORY_DISPOSABLE,
                context.getString(R.string.core_category_disposable),
                children = ::provideDisposableTracks
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

    /**
     * Factory function that creates [MediaItem]s from artists.
     */
    private val artistItemFactory: Artist.(
        MediaDescriptionCompat.Builder
    ) -> MediaItem = { builder ->
        val mediaId = encode(TYPE_ARTISTS, id.toString())
        val artistDescription = builder.setMediaId(mediaId)
            .setTitle(name)
            .setSubtitle(context.getString(R.string.artist_subtitle, albumCount, trackCount))
            .setIconUri(iconUri?.toUri())
            .setExtras(Bundle().apply {
                putInt(MediaItems.EXTRA_NUMBER_OF_TRACKS, trackCount)
            }).build()

        MediaItem(artistDescription, MediaItem.FLAG_BROWSABLE)
    }

    /**
     * Factory function that creates [MediaItem]s from albums.
     */
    private val albumItemFactory: Album.(
        MediaDescriptionCompat.Builder
    ) -> MediaItem = { builder ->
        val albumMediaId = encode(TYPE_ALBUMS, id.toString())
        val albumDescription = builder.setMediaId(albumMediaId)
            .setTitle(title)
            .setSubtitle(artist)
            .setIconUri(albumArtUri?.toUri())
            .setExtras(Bundle().apply {
                putInt(MediaItems.EXTRA_NUMBER_OF_TRACKS, trackCount)
            }).build()

        MediaItem(albumDescription, MediaItem.FLAG_BROWSABLE)
    }

    /**
     * Factory function that creates [MediaItem] from playlists.
     */
    private val playlistItemFactory: Playlist.(
        MediaDescriptionCompat.Builder
    ) -> MediaItem = { builder ->
        val mediaId = encode(TYPE_PLAYLISTS, id.toString())
        val playlistDescription = builder.setMediaId(mediaId)
            .setTitle(title)
            .setIconUri(iconUri)
            .build()

        MediaItem(playlistDescription, MediaItem.FLAG_BROWSABLE)
    }

    /**
     * Factory function that creates [MediaItem] from tracks.
     * Unlike other items, playable tracks may have different media ids for a same media file.
     *
     * Therefore, creating a track [MediaItem] requires to specify the type and category
     * of its parent in the browser tree so that the correct media item is assigned.
     */
    private val trackItemFactory: Track.(
        type: String,
        category: String,
        MediaDescriptionCompat.Builder
    ) -> MediaItem = { type, category, builder ->
        val mediaId = encode(type, category, id)
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

        MediaItem(description, MediaItem.FLAG_PLAYABLE)
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

    override suspend fun search(query: SearchQuery): List<MediaItem> {

        val results = when (query) {

            is SearchQuery.Artist -> {
                query.name?.toLowerCase()?.let { artistName ->
                    val artists = repository.getAllArtists()
                    singleTypeSearch(artistName, artists, Artist::name, artistItemFactory)
                }
            }

            is SearchQuery.Album -> {
                query.title?.toLowerCase()?.let { albumTitle ->
                    val albums = repository.getAllAlbums()
                    singleTypeSearch(albumTitle, albums, Album::title, albumItemFactory)
                }
            }

            is SearchQuery.Song -> {
                query.title?.toLowerCase()?.let { trackTitle ->
                    val tracks = repository.getAllTracks()
                    singleTypeSearch(trackTitle, tracks, Track::title) { builder ->
                        trackItemFactory(this, TYPE_TRACKS, CATEGORY_ALL, builder)
                    }
                }
            }

            is SearchQuery.Unspecified -> coroutineScope {
                val userQuery = query.userQuery
                val artists = async { repository.getAllArtists() }
                val albums = async { repository.getAllAlbums() }
                val tracks = async { repository.getAllTracks() }

                val searchResults = mutableListOf<ItemScore>()
                fuzzySearchTo(
                    searchResults,
                    userQuery,
                    artists.await(),
                    Artist::name,
                    artistItemFactory
                )
                fuzzySearchTo(
                    searchResults,
                    userQuery,
                    albums.await(),
                    Album::title,
                    albumItemFactory
                )
                fuzzySearchTo(
                    searchResults,
                    userQuery,
                    tracks.await(),
                    Track::title
                ) { track, builder ->
                    trackItemFactory(track, TYPE_TRACKS, CATEGORY_ALL, builder)
                }

                searchResults.sortByDescending(ItemScore::score)
                searchResults.map(ItemScore::media)
            }

            else -> null
        }

        return results.orEmpty()
    }

    /**
     * Holds the result of performing a fuzzy search of a pattern in a string.
     * Its properties are mutable by design, so that the same instance can be reused
     * when performing sequential fuzzy searches.
     */
    private class MatchResult {

        /**
         * Whether the pattern was found in the tested string.
         */
        var matched = false

        /**
         * Correspondence score.
         * The higher, the better the tested string matches the pattern.
         */
        var score = Int.MIN_VALUE
    }

    /**
     * Groups a media item with its correspondence score.
     */
    private class ItemScore(val media: MediaItem, val score: Int)

    /**
     * Perform a fuzzy search for the given [pattern] in [medias][availableMedias] of a single type.
     * Results are sorted in descending correspondence score.
     *
     * @param T The type of media.
     * @param pattern The pattern to search in available media.
     * @param availableMedias The list of media to be searched in.
     * @param availableMedias A set of all available media of a given type.
     * @param textProvider Function that retrieves the text string the pattern should be matched against.
     * @param itemFactory Factory function for creating media items from the available medias.
     */
    private fun <T : Any> singleTypeSearch(
        pattern: String,
        availableMedias: List<T>,
        textProvider: (T) -> String,
        itemFactory: T.(MediaDescriptionCompat.Builder) -> MediaItem
    ): List<MediaItem> {
        val searchResults = mutableListOf<ItemScore>().also {
            fuzzySearchTo(it, pattern, availableMedias, textProvider, itemFactory)
            it.sortByDescending(ItemScore::score)
        }

        return searchResults.map(ItemScore::media)
    }

    /**
     * Search a [pattern] in a [text] string.
     * Result of matching is then available in the passed [outResult].
     *
     * @param pattern Set of characters to be found in the [text] string.
     * @param text String that should be validated again the [pattern].
     * @param outResult An instance that will hold the result of the matching.
     * Its state is modified after calling this function.
     */
    private fun fuzzyMatch(pattern: String, text: String, outResult: MatchResult) {
        val matchPosition = text.indexOf(pattern)

        if (matchPosition < 0) {
            outResult.matched = false
            outResult.score = Int.MIN_VALUE
        } else {
            outResult.matched = true
            outResult.score = (BASE_SCORE - matchPosition - text.length)
        }
    }

    /**
     * Search all media in [availableMedias] that match the given fuzzy [pattern].
     * Each matching media is given a [correspondence score][ItemScore.score]
     * indicating how well it matched the pattern.
     * Matching media are then written to the provided [outResults].
     *
     * @param T The type of media.
     * @param outResults A list of media items with score to which matching items should be added.
     * @param pattern The pattern to search in media.
     * @param availableMedias A set of all available media of a given type.
     * @param textProvider Function that retrieves the text string the pattern should be matched against.
     * @param itemFactory Factory function for creating media items from the available medias.
     */
    private fun <T : Any> fuzzySearchTo(
        outResults: MutableList<ItemScore>,
        pattern: String,
        availableMedias: List<T>,
        textProvider: (T) -> String,
        itemFactory: (T, MediaDescriptionCompat.Builder) -> MediaItem
    ) {
        val builder = MediaDescriptionCompat.Builder()
        val matchResult = MatchResult()

        availableMedias.fold(outResults) { results, media ->
            fuzzyMatch(pattern, textProvider(media).toLowerCase(), matchResult)

            if (!matchResult.matched) results else {
                val item = itemFactory(media, builder)
                results += ItemScore(item, matchResult.score)
                results
            }
        }
    }

    override val updatedParentIds: Flowable<MediaId>
        get() = repository.changeNotifications.flatMap {
            if (it is ChangeNotification.AllTracks) Flowable.just(
                MediaId(TYPE_TRACKS, CATEGORY_ALL),
                MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED),
                MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED),
                MediaId(TYPE_TRACKS, CATEGORY_DISPOSABLE)
            ) else Flowable.just(it.mediaId)
        }

    private suspend fun provideAllTracks(fromIndex: Int, count: Int): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()

        return repository.getAllTracks().asSequence()
            .drop(fromIndex)
            .take(count)
            .mapTo(mutableListOf()) { track ->
                trackItemFactory(track, TYPE_TRACKS, CATEGORY_ALL, builder)
            }
    }

    private suspend fun provideMostRatedTracks(fromIndex: Int, count: Int): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()

        return usageManager.getMostRatedTracks().asSequence()
            .drop(fromIndex)
            .take(count)
            .mapTo(mutableListOf()) { track ->
                trackItemFactory(track, TYPE_TRACKS, CATEGORY_MOST_RATED, builder)
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
                trackItemFactory(track, TYPE_TRACKS, CATEGORY_RECENTLY_ADDED, builder)
            }
    }

    private suspend fun provideDisposableTracks(fromIndex: Int, count: Int): List<MediaItem>? {
        val builder = MediaDescriptionCompat.Builder()

        return usageManager.getDisposableTracks().asSequence()
            .drop(fromIndex)
            .take(count)
            .map { track ->
                val mediaId = MediaId(TYPE_TRACKS, CATEGORY_DISPOSABLE, track.trackId)
                val description = builder.setMediaId(mediaId.encoded)
                    .setTitle(track.title)
                    .setExtras(Bundle().apply {
                        putLong(MediaItems.EXTRA_FILE_SIZE, track.fileSizeBytes)
                        if (track.lastPlayedTime != null) {
                            putLong(MediaItems.EXTRA_LAST_PLAYED_TIME, track.lastPlayedTime)
                        }
                    })
                    .build()
                MediaItem(description, MediaItem.FLAG_PLAYABLE)
            }
            .toList()
    }

    private suspend fun provideAllAlbums(): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()

        return repository.getAllAlbums().map { album ->
            albumItemFactory(album, builder)
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
                    trackItemFactory(albumTrack, TYPE_ALBUMS, albumCategory, builder)
                }
                .takeUnless { it.isEmpty() }
        }
    }

    private suspend fun provideAllArtists(): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()

        return repository.getAllArtists().map { artist ->
            artistItemFactory(artist, builder)
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
                        albumItemFactory(album, builder)
                    }

                val artistTracks = asyncAllTracks.await().asSequence()
                    .filter { it.artistId == artistId }
                    .map { track ->
                        trackItemFactory(track, TYPE_ARTISTS, artistCategory, builder)
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
            playlistItemFactory(playlist, builder)
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
                    trackItemFactory(playlistTrack, TYPE_PLAYLISTS, playlistCategory, builder)
                }
        }
    }
}