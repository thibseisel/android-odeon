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

package fr.nihilus.music.service.browser

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.net.toUri
import fr.nihilus.music.core.database.playlists.Playlist
import fr.nihilus.music.core.database.spotify.TrackFeature
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_DISPOSABLE
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_MOST_RATED
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_POPULAR
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_RECENTLY_ADDED
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ALBUMS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ARTISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_PLAYLISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_SMART
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.core.media.MediaId.Builder.encode
import fr.nihilus.music.core.media.MediaItems
import fr.nihilus.music.service.ServiceScoped
import fr.nihilus.music.media.provider.Album
import fr.nihilus.music.media.provider.Artist
import fr.nihilus.music.media.provider.Track
import fr.nihilus.music.media.repo.ChangeNotification
import fr.nihilus.music.media.repo.MediaRepository
import fr.nihilus.music.media.usage.UsageManager
import fr.nihilus.music.service.R
import fr.nihilus.music.service.extensions.getResourceUri
import fr.nihilus.music.spotify.manager.FeatureFilter
import fr.nihilus.music.spotify.manager.SpotifyManager
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.Comparator

/**
 * The arbitrary maximum correspondence score for a fuzzy match.
 * When a string matches a fuzzy pattern, it is initially given this score.
 */
private const val BASE_SCORE = 100

/**
 * An increase of the search score that is attributed when the query matches
 * the start of the first word.
 */
private const val FIRST_WORD_BONUS = 30

private val ChangeNotification.mediaId: MediaId
    get() = when(this) {
        is ChangeNotification.AllTracks -> MediaId(TYPE_TRACKS, CATEGORY_ALL)
        is ChangeNotification.AllAlbums -> MediaId(TYPE_ALBUMS)
        is ChangeNotification.AllArtists -> MediaId(TYPE_ARTISTS)
        is ChangeNotification.AllPlaylists -> MediaId(TYPE_PLAYLISTS)
        is ChangeNotification.Album -> MediaId(TYPE_ALBUMS, albumId.toString())
        is ChangeNotification.Artist -> MediaId(TYPE_ARTISTS, artistId.toString())
        is ChangeNotification.Playlist -> MediaId(TYPE_PLAYLISTS, playlistId.toString())
    }

@ServiceScoped
internal class BrowserTreeImpl
@Inject constructor(
    private val context: Context,
    private val repository: MediaRepository,
    private val usageManager: UsageManager,
    private val spotifyManager: SpotifyManager
) : BrowserTree {

    private val albumTrackOrdering = Comparator<Track> { a, b ->
        val discNumberDiff = a.discNumber - b.discNumber
        if (discNumberDiff != 0) discNumberDiff else (a.trackNumber - b.trackNumber)
    }

    /**
     * The tree structure of the media browser.
     */
    private val tree = MediaTree(MediaId.ROOT) {
        rootName = context.getString(R.string.svc_browser_root_title)
        val res = context.resources

        type(
            TYPE_TRACKS,
            title = context.getString(R.string.svc_tracks_type_title)
        ) {

            category(
                CATEGORY_ALL,
                title = res.getString(R.string.svc_all_music),
                children = ::provideAllTracks
            )

            category(
                CATEGORY_MOST_RATED,
                title = res.getString(R.string.svc_most_rated),
                subtitle = res.getString(R.string.svc_most_rated_description),
                iconUri = res.getResourceUri(R.drawable.svc_ic_most_rated_128dp),
                children = ::provideMostRatedTracks
            )

            category(
                CATEGORY_POPULAR,
                title = context.getString(R.string.svc_category_popular),
                subtitle = context.getString(R.string.svc_category_popular_description),
                iconUri = null,
                children = ::provideMonthPopularTracks
            )

            category(
                CATEGORY_RECENTLY_ADDED,
                context.getString(R.string.svc_last_added),
                subtitle = res.getString(R.string.svc_recently_added_description),
                iconUri = res.getResourceUri(R.drawable.svc_ic_most_recent_128dp),
                children = ::provideRecentlyAddedTracks
            )

            category(
                CATEGORY_DISPOSABLE,
                context.getString(R.string.svc_category_disposable),
                children = ::provideDisposableTracks
            )
        }

        type(
            TYPE_ALBUMS,
            title = context.getString(R.string.svc_albums_type_title)
        ) {
            categories(provider = ::provideAllAlbums)
            categoryChildren(provider = ::provideAlbumTracks)
        }

        type(
            TYPE_ARTISTS,
            title = context.getString(R.string.svc_artists_type_title)
        ) {
            categories(provider = ::provideAllArtists)
            categoryChildren(provider = ::provideArtistChildren)
        }

        type(
            TYPE_PLAYLISTS,
            title = context.getString(R.string.svc_playlists_type_title)
        ) {
            categories(provider = ::provideAllPlaylists)
            categoryChildren(provider = ::providePlaylistTracks)
        }

        type(
            TYPE_SMART,
            title = context.getString(R.string.svc_smart_type_title)
        ) {
            category(
                "HAPPY",
                context.getString(R.string.svc_happy_category_title),
                children = ::provideHappyTracks
            )

            category(
                "PARTY",
                context.getString(R.string.svc_party_category_title),
                children = ::provideDanceableTracks
            )
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
            .setSubtitle(context.getString(R.string.svc_artist_subtitle, albumCount, trackCount))
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

    override suspend fun getChildren(parentId: MediaId, options: PaginationOptions?): List<MediaItem>? {
        // Take pagination into account when specified.
        val pageNumber = options?.page
            ?.coerceAtLeast(PaginationOptions.MINIMUM_PAGE_NUMBER)
            ?: PaginationOptions.DEFAULT_PAGE_NUMBER

        val pageSize = options?.size
            ?.coerceAtLeast(PaginationOptions.MINIMUM_PAGE_SIZE)
            ?: PaginationOptions.DEFAULT_PAGE_SIZE

        return tree.getChildren(parentId, pageNumber, pageSize)
    }

    override suspend fun getItem(itemId: MediaId): MediaItem? = tree.getItem(itemId)

    override suspend fun search(query: SearchQuery): List<MediaItem> {
        // Most songs and albums have an english title.
        // For the time being, we'll use english rules for matching text.
        val searchLocale = Locale.ENGLISH

        val results = when (query) {

            is SearchQuery.Artist -> {
                query.name?.toLowerCase(searchLocale)?.let { artistName ->
                    val artists = repository.getArtists()
                    singleTypeSearch(artistName, artists, Artist::name, artistItemFactory)
                }
            }

            is SearchQuery.Album -> {
                query.title?.toLowerCase(searchLocale)?.let { albumTitle ->
                    val albums = repository.getAlbums()
                    singleTypeSearch(albumTitle, albums, Album::title, albumItemFactory)
                }
            }

            is SearchQuery.Song -> {
                query.title?.toLowerCase(searchLocale)?.let { trackTitle ->
                    val tracks = repository.getTracks()
                    singleTypeSearch(trackTitle, tracks, Track::title) { builder ->
                        trackItemFactory(this, TYPE_TRACKS, CATEGORY_ALL, builder)
                    }
                }
            }

            is SearchQuery.Unspecified -> coroutineScope {
                val userQuery = query.userQuery.toLowerCase(searchLocale)
                val artists = async { repository.getArtists() }
                val albums = async { repository.getAlbums() }
                val tracks = async { repository.getTracks() }

                val searchResults = mutableListOf<ItemScore>()
                fuzzySearchTo(searchResults, userQuery, artists.await(), Artist::name, artistItemFactory)
                fuzzySearchTo(searchResults, userQuery, albums.await(), Album::title, albumItemFactory)
                fuzzySearchTo(searchResults, userQuery, tracks.await(), Track::title) { track, builder ->
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
     *
     * @param pattern Set of characters to be found in the [text] string.
     * @param text String that should be validated again the [pattern].
     *
     * @return The matching score. Higher is better.
     * A score of [Int.MIN_VALUE] indicates that the pattern haven't matched.
     */
    private fun fuzzyMatch(pattern: String, text: String): Int {
        val matchPosition = text.indexOf(pattern)

        if (matchPosition < 0) {
            return Int.MIN_VALUE
        } else {
            var score = BASE_SCORE - matchPosition - text.length
            if (matchPosition == 0) {
                // The query matched the start of the first word, give it a bonus.
                score += FIRST_WORD_BONUS
            }

            return score
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
        availableMedias.fold(outResults) { results, media ->

            when(val score = fuzzyMatch(pattern, textProvider(media).toLowerCase(Locale.ENGLISH))) {
                Int.MIN_VALUE -> results
                else -> {
                    val item = itemFactory(media, builder)
                    results += ItemScore(item, score)
                    results
                }
            }
        }
    }

    override val updatedParentIds: Flow<MediaId>
        get() = repository.changeNotifications.transform {
            when (it) {
                is ChangeNotification.AllTracks -> {
                    emit(MediaId(TYPE_TRACKS, CATEGORY_ALL))
                    emit(MediaId(TYPE_TRACKS, CATEGORY_MOST_RATED))
                    emit(MediaId(TYPE_TRACKS, CATEGORY_POPULAR))
                    emit(MediaId(TYPE_TRACKS, CATEGORY_RECENTLY_ADDED))
                    emit(MediaId(TYPE_TRACKS, CATEGORY_DISPOSABLE))
                }
                else -> emit(it.mediaId)
            }
        }

    private suspend fun provideAllTracks(fromIndex: Int, count: Int): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()

        return repository.getTracks().asSequence()
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
    
    private suspend fun provideMonthPopularTracks(fromIndex: Int, count: Int): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()
        
        return usageManager.getPopularTracksSince(30, TimeUnit.DAYS).asSequence()
            .drop(fromIndex)
            .take(count)
            .mapTo(mutableListOf()) { track ->
                trackItemFactory(track, TYPE_TRACKS, CATEGORY_POPULAR, builder)
            }
    }

    private suspend fun provideRecentlyAddedTracks(fromIndex: Int, count: Int): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()

        return repository.getTracks().asSequence()
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
                        track.lastPlayedTime?.let { lastPlayedTime ->
                            putLong(MediaItems.EXTRA_LAST_PLAYED_TIME, lastPlayedTime)
                        }
                    })
                    .build()
                MediaItem(description, MediaItem.FLAG_PLAYABLE)
            }
            .toList()
    }

    private suspend fun provideAllAlbums(): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()

        return repository.getAlbums().map { album ->
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

            repository.getTracks().asSequence()
                .filter { it.albumId == albumId }
                .sortedWith(albumTrackOrdering)
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

        return repository.getArtists().map { artist ->
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

                val asyncAllAlbums = async { repository.getAlbums() }
                val asyncAllTracks = async { repository.getTracks() }

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
        val allPlaylists = repository.getPlaylists()

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

    private suspend fun provideHappyTracks(startIndex: Int, count: Int): List<MediaItem> {
        val happyFilters = listOf(
            FeatureFilter.OnRange(TrackFeature::valence, 0.65f, 1.0f)
        )

        val builder = MediaDescriptionCompat.Builder()
        return spotifyManager.findTracksHavingFeatures(happyFilters).asSequence()
            .drop(startIndex)
            .take(count)
            .mapTo(mutableListOf()) { (track, _) ->
                trackItemFactory(track, TYPE_SMART, "HAPPY", builder)
            }
    }

    private suspend fun provideDanceableTracks(fromIndex: Int, count: Int): List<MediaItem> {
        val partyFilters = listOf(
            FeatureFilter.OnRange(TrackFeature::danceability, 0.7f, 1.0f),
            FeatureFilter.OnRange(TrackFeature::energy, 0.5f, 1.0f)
        )

        val builder = MediaDescriptionCompat.Builder()
        return spotifyManager.findTracksHavingFeatures(partyFilters).asSequence()
            .drop(fromIndex)
            .take(count)
            .mapTo(mutableListOf()) { (track, _) ->
                trackItemFactory(track, TYPE_SMART, "PARTY", builder)
            }
    }
}