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

package fr.nihilus.music.spotify.service

import com.squareup.moshi.Moshi
import fr.nihilus.music.spotify.model.*
import io.kotlintest.*
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.numerics.shouldBeGreaterThanOrEqual
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.matchers.types.shouldBeTypeOf
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.matchers.withClue
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import kotlin.test.Test

private const val TEST_USER_AGENT = "SpotifyServiceTest/1.0.0 KtorHttpClient/1.2.4"
private const val TEST_CLIENT_ID = "client_id"
private const val TEST_CLIENT_SECRET = "client_secret"

private val VALID_TOKEN = OAuthToken(TEST_TOKEN_STRING, "Bearer", 3600)

@ExperimentalCoroutinesApi
class SpotifyServiceTest {

    private val moshi = Moshi.Builder().build()

    private val accounts = mockk<SpotifyAccountsService>()

    private fun spotifyService(
        token: OAuthToken? = VALID_TOKEN,
        handler: suspend (HttpRequestData) -> HttpResponseData
    ): SpotifyService = SpotifyServiceImpl(
        MockEngine(handler),
        moshi,
        accounts,
        TEST_USER_AGENT,
        TEST_CLIENT_ID,
        TEST_CLIENT_SECRET,
        token
    )

    private fun spotifyService(
        engine: MockEngine,
        token: OAuthToken? = VALID_TOKEN
    ): SpotifyService = SpotifyServiceImpl(
        engine,
        moshi,
        accounts,
        TEST_USER_AGENT,
        TEST_CLIENT_ID,
        TEST_CLIENT_SECRET,
        token
    )

    @Test
    fun `Given no token, when calling any endpoint then authenticate`() = runBlockingTest {
        coEvery { accounts.authenticate(any(), any()) } returns OAuthToken(TEST_TOKEN_STRING, "Bearer", 3600)
        val unauthenticatedService = spotifyService(token = null) {
            respondFile("artists/12Chz98pHFMPJEknJQMWvI.json")
        }

        unauthenticatedService.getArtist("12Chz98pHFMPJEknJQMWvI")

        coVerify(exactly = 1) { accounts.authenticate(TEST_CLIENT_ID, TEST_CLIENT_SECRET) }
        confirmVerified(accounts)
    }

    @Test
    fun `Given failed authentication, when calling any endpoint then fail with AuthenticationException`() = runBlockingTest {
        coEvery { accounts.authenticate(any(), any()) } throws AuthenticationException(
            "invalid_client",
            "Invalid client"
        )

        val failedAuthService = spotifyService(token = null) {
            respondJsonError(HttpStatusCode.Unauthorized, "No token provided")
        }

        val authFailure = shouldThrow<AuthenticationException> {
            failedAuthService.getArtist("12Chz98pHFMPJEknJQMWvI")
        }

        authFailure.error shouldBe "invalid_client"
        authFailure.description shouldBe "Invalid client"
    }

    @Test
    fun `Given valid token, when calling any endpoint then send it as Authorization`() = runBlockingTest {
        val validToken = OAuthToken(TEST_TOKEN_STRING, "Bearer", 3600)
        val authenticatedService = spotifyService(validToken) { request ->
            // Verify that the Authorization header is present.
            request.headers[HttpHeaders.Authorization] shouldBe "Bearer $TEST_TOKEN_STRING"
            respondFile("artists/12Chz98pHFMPJEknJQMWvI.json")
        }

        shouldNotThrow<AuthenticationException> {
            authenticatedService.getArtist("12Chz98pHFMPJEknJQMWvI")
        }

        // Verify that no authentication is performed when not required.
        coVerify(exactly = 0) { accounts.authenticate(any(), any()) }
        confirmVerified(accounts)
    }

    @Test
    fun `Given expired token, when calling any endpoint then renew token and retry`() = runBlockingTest {
        coEvery { accounts.authenticate(any(), any()) } returns OAuthToken(TEST_TOKEN_STRING, "Bearer", 3600)

        val oldTokenSequence = "3IjZmZGNjZTZ1EDN2MTO5QmZkJjN0QTM"
        val expiredToken = OAuthToken(oldTokenSequence, "Bearer", 0)

        // The mock server returns a different response depending on the received token.
        val expiredService = spotifyService(token = expiredToken) { request ->
            val authorization = request.headers[HttpHeaders.Authorization]

            if (authorization == "Bearer $oldTokenSequence") {
                // Fail when receiving the old token.
                respondJsonError(HttpStatusCode.Unauthorized, "The access token expired")
            } else {
                // Check that the newly generated token has been used.
                authorization shouldBe "Bearer $TEST_TOKEN_STRING"
                respondFile("artists/12Chz98pHFMPJEknJQMWvI.json")
            }
        }

        expiredService.getArtist("12Chz98pHFMPJEknJQMWvI")

        coVerify(exactly = 1) { accounts.authenticate(TEST_CLIENT_ID, TEST_CLIENT_SECRET) }
        confirmVerified(accounts)
    }

    @Test
    fun `Given reached rate limit, when calling any endpoint then retry after the given delay`() = runBlockingTest {
        val rateLimitedServer = givenReachedRateLimit(retryAfter = 5)
        val apiClient = spotifyService(rateLimitedServer)

        val artist = apiClient.getArtist("12Chz98pHFMPJEknJQMWvI")
        artist.shouldBeTypeOf<Resource.Loaded<Artist>>()

        withClue("Client should wait at least the given Retry-After time before re-issuing the request") {
            currentTime shouldBeGreaterThanOrEqual 5000L
        }
    }

    @Test
    fun `When getting an artist then call artists endpoint with its id`() = runBlockingTest {
        val apiClient = spotifyService { request ->
            request shouldGetOnSpotifyEndpoint "/v1/artists/12Chz98pHFMPJEknJQMWvI"
            respondFile("artists/12Chz98pHFMPJEknJQMWvI.json")
        }

        val artistResource = apiClient.getArtist("12Chz98pHFMPJEknJQMWvI")
        artistResource.shouldBeTypeOf<Resource.Loaded<Artist>> { (artist) ->
            artist.id shouldBe "12Chz98pHFMPJEknJQMWvI"
            artist.name shouldBe "Muse"
            artist.popularity shouldBe 82
            artist.genres.shouldContainExactlyInAnyOrder("modern rock", "permanent wave", "piano rock", "post-grunge", "rock")
            artist.images.shouldContainExactly(
                Image("https://i.scdn.co/image/17f00ec7613d733f2dd88de8f2c1628ea5f9adde", 320, 320)
            )
        }
    }

    @Test
    fun `When getting an unknown artist then return a NotFound resource`() = runBlockingTest {
        val apiClient = spotifyService {
            respondJsonError(HttpStatusCode.NotFound, "non existing id")
        }

        val artistResource = apiClient.getArtist("non_existing_artist_id")
        artistResource.shouldBeTypeOf<Resource.NotFound>()
    }

    @Test
    fun `When getting several artists then call artists endpoint with their ids`() = runBlockingTest {
        val requestedArtistIds = listOf("12Chz98pHFMPJEknJQMWvI", "7jy3rLJdDQY21OgRLCZ9sD")

        val apiClient = spotifyService { request ->
            request shouldGetOnSpotifyEndpoint "/v1/artists"

            val requestedIds = request.url.parameters[SpotifyService.QUERY_IDS]
            requestedIds shouldBe "12Chz98pHFMPJEknJQMWvI,7jy3rLJdDQY21OgRLCZ9sD"

            respondFile("artists/several.json")
        }

        val artistsResource = apiClient.getSeveralArtists(requestedArtistIds)
        artistsResource.shouldBeTypeOf<Resource.Loaded<List<Artist?>>> { (artists) ->
            artists shouldHaveSize 2

            artists[0].should {
                it.shouldNotBeNull()
                it.id shouldBe "12Chz98pHFMPJEknJQMWvI"
                it.name shouldBe "Muse"
                it.popularity shouldBe 82
                it.genres.shouldContainExactly("modern rock", "permanent wave", "piano rock", "post-grunge", "rock")
                it.images.shouldContainExactly(
                    Image("https://i.scdn.co/image/17f00ec7613d733f2dd88de8f2c1628ea5f9adde", 320, 320)
                )
            }

            artists[1].should {
                it.shouldNotBeNull()
                it.id shouldBe "7jy3rLJdDQY21OgRLCZ9sD"
                it.name shouldBe "Foo Fighters"
                it.popularity shouldBe 82
                it.genres.shouldContainExactly("alternative metal", "alternative rock", "modern rock", "permanent wave", "post-grunge", "rock")
                it.images.shouldContainExactly(
                    Image("https://i.scdn.co/image/c508060cb93f3d2f43ad0dc38602eebcbe39d16d", 320, 320)
                )
            }
        }
    }

    @Test
    fun `When getting an artist's albums, then fetch its albums page per page`() = runBlockingTest {
        val mockServer = MockEngine(MockEngineConfig().apply {
            addHandler {
                it shouldGetOnSpotifyEndpoint "/v1/artists/12Chz98pHFMPJEknJQMWvI/albums"
                respondFile("artists/albums_page_1.json")
            }

            addHandler {
                it shouldGetOnSpotifyEndpoint "/v1/artists/12Chz98pHFMPJEknJQMWvI/albums"
                it.url.parameters[SpotifyService.QUERY_LIMIT] shouldBe "2"
                it.url.parameters[SpotifyService.QUERY_OFFSET] shouldBe "2"

                respondFile("artists/albums_page_2.json")
            }
        })

        val apiClient = spotifyService(mockServer)

        val artistAlbums = apiClient.getArtistAlbums("12Chz98pHFMPJEknJQMWvI").toList()
        artistAlbums shouldHaveSize 4

        with(artistAlbums[0]) {
            id shouldBe "5OZgDtx180ZZPMpm36J2zC"
            name shouldBe "Simulation Theory (Super Deluxe)"
            releaseDate shouldBe "2018-11-09"
            releaseDatePrecision shouldBe "day"
            images.shouldContainExactly(
                Image("https://i.scdn.co/image/0b2a261f7bec0ed109a149316d116c15ca72e5ef", 300, 300)
            )
        }

        artistAlbums[1].id shouldBe "2wart5Qjnvx1fd7LPdQxgJ"
        artistAlbums[2].id shouldBe "3KuXEGcqLcnEYWnn3OEGy0"
        artistAlbums[3].id shouldBe "0eFHYz8NmK75zSplL5qlfM"
    }

    @Test
    fun `When getting albums of an unknown artist, then return an empty flow`() = runBlockingTest {
        val apiClient = spotifyService {
            respondJsonError(HttpStatusCode.NotFound, "non existing id")
        }

        val artistAlbums = apiClient.getArtistAlbums("unknown_artist_id").toList()
        artistAlbums.shouldBeEmpty()
    }

    @Test
    fun `When getting an album then call albums endpoint with its id`() = runBlockingTest {
        val apiClient = spotifyService { request ->
            request shouldGetOnSpotifyEndpoint "/v1/albums/6KMkuqIwKkwUhUYRPL6dUc"
            respondFile("albums/7jy3rLJdDQY21OgRLCZ9sD.json")
        }

        val albumResource = apiClient.getAlbum("6KMkuqIwKkwUhUYRPL6dUc")
        albumResource.shouldBeTypeOf<Resource.Loaded<Album>> { (album) ->
            album.id shouldBe "6KMkuqIwKkwUhUYRPL6dUc"
            album.name shouldBe "Concrete and Gold"
            album.releaseDate shouldBe "2017-09-15"
            album.releaseDatePrecision shouldBe "day"
            album.images.shouldContainExactly(
                Image("https://i.scdn.co/image/466a21e8c6f72e540392ae76a94e01c876a8f193", 300, 300)
            )
        }
    }

    @Test
    fun `When getting an unknown album then fail with a NotFound resource`() = runBlockingTest {
        val apiClient = spotifyService {
            respondJsonError(HttpStatusCode.NotFound, "non existing id")
        }

        val albumResource = apiClient.getAlbum("unknown_album_id")
        albumResource.shouldBeTypeOf<Resource.NotFound>()
    }

    @Test
    fun `When getting several albums then call albums endpoint with their ids`() = runBlockingTest {
        val requestedAlbumIds = listOf("5OZgDtx180ZZPMpm36J2zC", "6KMkuqIwKkwUhUYRPL6dUc")

        val apiClient = spotifyService { request ->
            request shouldGetOnSpotifyEndpoint "/v1/albums"

            val receivedIds = request.url.parameters[SpotifyService.QUERY_IDS]
            receivedIds shouldBe "5OZgDtx180ZZPMpm36J2zC,6KMkuqIwKkwUhUYRPL6dUc"

            respondFile("albums/several.json")
        }

        val resource = apiClient.getSeveralAlbums(requestedAlbumIds)
        resource.shouldBeTypeOf<Resource.Loaded<List<Album?>>> { (albums) ->
            albums shouldHaveSize 2

            albums[0].should {
                it.shouldNotBeNull()
                it.id shouldBe "5OZgDtx180ZZPMpm36J2zC"
                it.name shouldBe "Simulation Theory (Super Deluxe)"
                it.releaseDate shouldBe "2018-11-09"
                it.releaseDatePrecision shouldBe "day"
                it.images shouldHaveSize 1
            }

            albums[1].should {
                it.shouldNotBeNull()
                it.id shouldBe "6KMkuqIwKkwUhUYRPL6dUc"
                it.name shouldBe "Concrete and Gold"
                it.releaseDate shouldBe "2017-09-15"
                it.releaseDatePrecision shouldBe "day"
                it.images shouldHaveSize 1
            }
        }
    }

    @Test
    fun `When getting an album's tracks then fetch its tracks page per page`() = runBlockingTest {
        val mockServer = MockEngine(MockEngineConfig().apply {
            addHandler { firstPage ->
                firstPage shouldGetOnSpotifyEndpoint "/v1/albums/5OZgDtx180ZZPMpm36J2zC/tracks"
                respondFile("albums/tracks_page_1.json")
            }

            addHandler { secondPage ->
                secondPage shouldGetOnSpotifyEndpoint "/v1/albums/5OZgDtx180ZZPMpm36J2zC/tracks"
                secondPage.url.parameters[SpotifyService.QUERY_LIMIT] shouldBe "2"
                secondPage.url.parameters[SpotifyService.QUERY_OFFSET] shouldBe "2"

                respondFile("albums/tracks_page_2.json")
            }
        })

        val apiClient = spotifyService(mockServer)
        val albumTracks = apiClient.getAlbumTracks("5OZgDtx180ZZPMpm36J2zC").toList()

        albumTracks shouldHaveSize 4

        with(albumTracks[0]) {
            id shouldBe "7f0vVL3xi4i78Rv5Ptn2s1"
            name shouldBe "Algorithm"
            duration shouldBe 245960
            discNumber shouldBe 1
            trackNumber shouldBe 1
            explicit shouldBe false
        }

        albumTracks[1].id shouldBe "0dMYPDqcI4ca4cjqlmp9mE"
        albumTracks[2].id shouldBe "3eSyMBd7ERw68NVB3jlRmW"
        albumTracks[3].id shouldBe "2sHLWUTiaBPGOIZinpqO4C"
    }

    @Test
    fun `When getting tracks of an unknown album then return an empty flow`() = runBlockingTest {
        val apiClient = spotifyService {
            respondJsonError(HttpStatusCode.NotFound, "non existing id")
        }

        val albumTracks = apiClient.getAlbumTracks("unknown_album_id").toList()
        albumTracks.shouldBeEmpty()
    }

    @Test
    fun `When getting track detail then call tracks endpoint with its id`() = runBlockingTest {
        val requestedTrackId = "7f0vVL3xi4i78Rv5Ptn2s1"

        val apiClient = spotifyService { request ->
            request shouldGetOnSpotifyEndpoint "/v1/tracks/$requestedTrackId"
            respondFile("tracks/7f0vVL3xi4i78Rv5Ptn2s1.json")
        }

        val resource = apiClient.getTrack(requestedTrackId)
        resource.shouldBeTypeOf<Resource.Loaded<Track>> { (track) ->
            track.id shouldBe requestedTrackId
            track.name shouldBe "Algorithm"
            track.discNumber shouldBe 1
            track.trackNumber shouldBe 1
            track.explicit shouldBe false
        }
    }

    @Test
    fun `When getting an unknown track then fail with a NotFound resource`() = runBlockingTest {
        val apiClient = spotifyService {
            respondJsonError(HttpStatusCode.NotFound, "non existing id")
        }

        val resource = apiClient.getTrack("unknown_track_id")
        resource.shouldBeTypeOf<Resource.NotFound>()
    }

    @Test
    fun `When getting several tracks then call tracks endpoint with their ids`() = runBlockingTest {
        val requestedTrackIds = listOf("7f0vVL3xi4i78Rv5Ptn2s1", "0dMYPDqcI4ca4cjqlmp9mE")

        val apiClient = spotifyService { request ->
            request shouldGetOnSpotifyEndpoint "/v1/tracks"

            val receivedTrackIds = request.url.parameters[SpotifyService.QUERY_IDS]
            receivedTrackIds shouldBe "7f0vVL3xi4i78Rv5Ptn2s1,0dMYPDqcI4ca4cjqlmp9mE"

            respondFile("tracks/several.json")
        }

        val tracks = apiClient.getSeveralTracks(requestedTrackIds)
        tracks.shouldBeTypeOf<Resource.Loaded<List<Track?>>> { (tracks) ->
            tracks shouldHaveSize 2
            tracks[0].should {
                it.shouldNotBeNull()
                it.id shouldBe "7f0vVL3xi4i78Rv5Ptn2s1"
                it.name shouldBe "Algorithm"
                it.discNumber shouldBe 1
                it.trackNumber shouldBe 1
                it.duration shouldBe 245960
                it.explicit shouldBe false
            }

            tracks[1].should {
                it.shouldNotBeNull()
                it.id shouldBe "0dMYPDqcI4ca4cjqlmp9mE"
                it.name shouldBe "The Dark Side"
                it.discNumber shouldBe 1
                it.trackNumber shouldBe 2
                it.duration shouldBe 227213
                it.explicit shouldBe false
            }
        }
    }

    @Test
    fun `When getting features of a track then call audio-features endpoint with that track's id`() = runBlockingTest {
        val requestedTrackId = "7f0vVL3xi4i78Rv5Ptn2s1"

        val apiClient = spotifyService { request ->
            request shouldGetOnSpotifyEndpoint "/v1/audio-features/$requestedTrackId"
            respondFile("features/7f0vVL3xi4i78Rv5Ptn2s1.json")
        }

        val audioFeatures = apiClient.getTrackFeatures(requestedTrackId)
        audioFeatures.shouldBeTypeOf<Resource.Loaded<AudioFeature>> { (feature) ->
            feature.id shouldBe requestedTrackId
            feature.mode shouldBe MusicalMode.MAJOR
            feature.key shouldBe Pitch.D
            feature.tempo shouldBe 170.057f
            feature.signature shouldBe 4
            feature.loudness shouldBe -4.56f
            feature.energy shouldBe 0.923f
            feature.danceability shouldBe 0.522f
            feature.instrumentalness shouldBe 0.017f
            feature.speechiness shouldBe 0.0539f
            feature.acousticness shouldBe 0.0125f
            feature.liveness shouldBe 0.0854f
            feature.valence shouldBe 0.595f
        }
    }

    @Test
    fun `When getting features of an unknown track then fail with a NotFound resource`() = runBlockingTest {
        val apiClient = spotifyService {
            respondJsonError(HttpStatusCode.NotFound, "non existing id")
        }


        val resource = apiClient.getTrackFeatures("unknown_track_id")
        resource.shouldBeTypeOf<Resource.NotFound>()
    }

    @Test
    fun `When getting several tracks' features then call audio-features endpoint with their ids`() = runBlockingTest {
        val requestedIds = listOf("7f0vVL3xi4i78Rv5Ptn2s1", "5lnsL7pCg0fQKcWnlkD1F0")

        val apiClient = spotifyService { request ->
            request shouldGetOnSpotifyEndpoint "/v1/audio-features"

            val receivedTrackIds = request.url.parameters[SpotifyService.QUERY_IDS]
            receivedTrackIds shouldBe "7f0vVL3xi4i78Rv5Ptn2s1,5lnsL7pCg0fQKcWnlkD1F0"

            respondFile("features/several.json")
        }

        val resource = apiClient.getSeveralTrackFeatures(requestedIds)
        resource.shouldBeTypeOf<Resource.Loaded<List<AudioFeature?>>> { (feature) ->
            feature shouldHaveSize 2

            feature[0].should {
                it.shouldNotBeNull()
                it.id shouldBe "7f0vVL3xi4i78Rv5Ptn2s1"
                it.mode shouldBe MusicalMode.MAJOR
                it.key shouldBe Pitch.D
                it.tempo shouldBe 170.057f
                it.signature shouldBe 4
                it.loudness shouldBe -4.56f
                it.energy shouldBe 0.923f
                it.danceability shouldBe 0.522f
                it.instrumentalness shouldBe 0.017f
                it.speechiness shouldBe 0.0539f
                it.acousticness shouldBe 0.0125f
                it.liveness shouldBe 0.0854f
                it.valence shouldBe 0.595f
            }

            feature[1].should {
                it.shouldNotBeNull()
                it.id shouldBe "5lnsL7pCg0fQKcWnlkD1F0"
                it.mode shouldBe MusicalMode.MAJOR
                it.key shouldBe Pitch.G
                it.tempo shouldBe 142.684f
                it.signature shouldBe 4
                it.loudness shouldBe -8.245f
                it.energy shouldBe 0.631f
                it.danceability shouldBe 0.324f
                it.instrumentalness shouldBe 0.0459f
                it.speechiness shouldBe 0.0407f
                it.acousticness shouldBe 0.00365f
                it.liveness shouldBe 0.221f
                it.valence shouldBe 0.346f
            }
        }
    }

    @Test
    fun `When searching, then call search endpoint with corresponding type`() = runBlockingTest {
        TODO("Rewrite the test to adhere to the new API.")
        /*val searchClient = spotifyService { request ->
            request shouldGetOnSpotifyEndpoint "v1/search"

            val parameters = request.url.parameters
            parameters[SpotifyService.QUERY_Q] shouldBe "rammstein"
            val receivedSearchTypes = parameters[SpotifyService.QUERY_TYPE]
            receivedSearchTypes shouldBe "track,album,artist"

            respondJson(SEARCH_RESULTS)
        }

        val results = searchClient.search("rammstein", setOf("track", "album", "artist"))
        results.albums.should { paginatedAlbums ->
            paginatedAlbums.total shouldBe 1
            paginatedAlbums.items shouldHaveSize 1
            paginatedAlbums.items[0].should {
                it.id shouldBe "1LoyJQVHPLHE3fCCS8Juek"
                it.name shouldBe "RAMMSTEIN"
                it.releaseDate shouldBe "2019-05-17"
                it.releaseDatePrecision shouldBe "day"

                it.images.shouldContainExactly(
                    Image("https://i.scdn.co/image/389c1df3f21fa93570dde0b75332e75ab91bd878", 300, 300)
                )
            }
        }

        results.artists.should { paginatedArtists ->
            paginatedArtists.total shouldBe 1
            paginatedArtists.items shouldHaveSize 1
            paginatedArtists.items[0].should {
                it.id shouldBe "6wWVKhxIU2cEi0K81v7HvP"
                it.name shouldBe "Rammstein"
                it.popularity shouldBe 87
                it.genres.shouldContainExactly(
                    "alternative metal",
                    "german metal",
                    "industrial",
                    "industrial metal",
                    "industrial rock",
                    "neue deutsche harte"
                )

                it.images.shouldContainExactly(
                    Image("https://i.scdn.co/image/d7bba2e8eb624d93d8cc7cb57d9ba5fb35f0f901", 320, 320)
                )
            }
        }

        results.tracks.should { paginatedTracks ->
            paginatedTracks.total shouldBe 1
            paginatedTracks.items shouldHaveSize 1

            paginatedTracks.items[0].should {
                it.id shouldBe "5vZ4IeUenK2cHub2d7yfWk"
                it.name shouldBe "RADIO"
                it.discNumber shouldBe 1
                it.trackNumber shouldBe 2
                it.duration shouldBe 277397
                it.explicit shouldBe false
            }
        }*/
    }

    @Test
    fun `When requesting too much resources at one time, then fail with IllegalArgumentException`() = runBlockingTest {
        val apiClient = spotifyService {
            fail("Expected no network call, but called endpoint ${it.url.encodedPath}.")
        }

        val artistIds = List(51) { "12Chz98pHFMPJEknJQMWvI" }
        shouldThrow<IllegalArgumentException> { apiClient.getSeveralArtists(artistIds) }

        val albumIds = List(21) { "5OZgDtx180ZZPMpm36J2zC" }
        shouldThrow<IllegalArgumentException> { apiClient.getSeveralAlbums(albumIds) }

        val trackIds = List(51) { "7f0vVL3xi4i78Rv5Ptn2s1" }
        shouldThrow<IllegalArgumentException> { apiClient.getSeveralTracks(trackIds) }

        val trackFeatureIds = List(101) { "7f0vVL3xi4i78Rv5Ptn2s1" }
        shouldThrow<IllegalArgumentException> { apiClient.getSeveralTrackFeatures(trackFeatureIds) }
    }

    @Test
    fun `When calling any endpoint, then send the provided UserAgent`() = runBlockingTest {
        val apiClient = spotifyService { request ->
            request.headers[HttpHeaders.UserAgent] shouldBe TEST_USER_AGENT
            respondFile("tracks/7f0vVL3xi4i78Rv5Ptn2s1.json")
        }

        apiClient.getTrack("7f0vVL3xi4i78Rv5Ptn2s1")
    }

    @Test
    fun `Given any single resource endpoint, when receiving an error then return a Failed resource`() = runBlockingTest {
        val apiClient = spotifyService {
            respondJsonError(
                HttpStatusCode.InternalServerError,
                "Whoops!"
            )
        }

        val resource = apiClient.getArtist("12Chz98pHFMPJEknJQMWvI")
        resource.shouldBeTypeOf<Resource.Failed> {
            it.status shouldBe 500
            it.message shouldBe "Whoops!"
        }
    }

    @Test
    fun `Given any several resource endpoint, when receiving an error then return a Failed resource`() = runBlockingTest {
        val apiClient = spotifyService {
            respondJsonError(
                HttpStatusCode.InternalServerError,
                "Whoops!"
            )
        }

        val resource = apiClient.getSeveralArtists(
            listOf("12Chz98pHFMPJEknJQMWvI", "7jy3rLJdDQY21OgRLCZ9sD")
        )

        resource.shouldBeTypeOf<Resource.Failed> {
            it.status shouldBe 500
            it.message shouldBe "Whoops!"
        }
    }

    @Test
    fun `Given any paginated endpoint, when receiving an error then throw an ApiException`() = runBlockingTest {
        val apiClient = spotifyService {
            respondJsonError(
                HttpStatusCode.InternalServerError,
                "Whoops!"
            )
        }

        val apiException = shouldThrow<ApiException> {
            apiClient.getArtistAlbums("12Chz98pHFMPJEknJQMWvI").toList()
        }

        apiException.status shouldBe 500
        apiException.description shouldBe "Whoops!"
    }

    private fun givenReachedRateLimit(retryAfter: Int): MockEngine {
        val engineConfig = MockEngineConfig()
        var firstRequest: HttpRequestData? = null

        engineConfig.addHandler { rateLimitedRequest ->
            firstRequest = rateLimitedRequest
            respond(
                jsonApiError(
                    HttpStatusCode.TooManyRequests,
                    "Rate limitation has been exceeded. Retry later."
                ),
                HttpStatusCode.TooManyRequests,
                headersOf(
                    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                    HttpHeaders.RetryAfter to listOf(retryAfter.toString())
                )
            )
        }

        engineConfig.addHandler { retriedRequest ->
            val originalRequest = firstRequest ?: fail("The request should have been issued before re-attempted.")
            // Check that the retried request is the same as the failed one.
            retriedRequest.method shouldBe originalRequest.method
            retriedRequest.url shouldBe originalRequest.url
            retriedRequest.headers shouldBe originalRequest.headers

            respondFile("artists/12Chz98pHFMPJEknJQMWvI.json")
        }

        return MockEngine(engineConfig)
    }
}

/**
 * Generate the response of the Spotify Web API in case of error.
 *
 * @param status The status code associated with the response. It should be a valid error code (>= 400).
 * @param message The message provided as the `error` property of the JSON response body.
 */
private fun respondJsonError(status: HttpStatusCode, message: String = status.description) =
    respondJson(
        jsonApiError(status, message), status
    )

/**
 * Asserts that this request is an HTTP GET Request on the Spotify Web API (api.spotify.com)
 * on the specified [endpoint][spotifyApiEndpoint].
 *
 * @param spotifyApiEndpoint The path that should be requested on the server.
 */
private infix fun HttpRequestData.shouldGetOnSpotifyEndpoint(spotifyApiEndpoint: String) {
    method shouldBe HttpMethod.Get
    url.host shouldBe "api.spotify.com"
    url.encodedPath shouldBe spotifyApiEndpoint
}