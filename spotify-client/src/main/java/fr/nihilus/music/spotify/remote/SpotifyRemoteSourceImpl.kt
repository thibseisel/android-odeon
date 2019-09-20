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

package fr.nihilus.music.spotify.remote

import com.squareup.moshi.Moshi
import fr.nihilus.music.spotify.OAuthToken
import fr.nihilus.music.spotify.RetryAfter
import fr.nihilus.music.spotify.model.Album
import fr.nihilus.music.spotify.model.Artist
import fr.nihilus.music.spotify.model.AudioFeatures
import fr.nihilus.music.spotify.model.Paging
import fr.nihilus.music.spotify.model.SearchResults
import fr.nihilus.music.spotify.model.SpotifyError
import fr.nihilus.music.spotify.model.Track
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.features.UserAgent
import io.ktor.client.features.defaultRequest
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.http.*
import org.jetbrains.annotations.TestOnly

private const val QUERY_Q = "q"
private const val QUERY_IDS = "ids"
private const val QUERY_LIMIT = "limit"
private const val QUERY_OFFSET = "offset"
private const val QUERY_TYPE = "type"
private const val QUERY_INCLUDE_GROUPS = "include_groups"

internal class SpotifyRemoteSourceImpl(
    engine: HttpClientEngine,
    private val moshi: Moshi,
    private val accountsService: SpotifyAccountsService,
    userAgent: String,
    private val clientKey: String,
    private val clientSecret: String
): SpotifyRemoteSource {

    private val errorAdapter = moshi.adapter(SpotifyError::class.java)

    private val http = HttpClient(engine) {
        expectSuccess = false

        install(UserAgent) {
            agent = userAgent
        }

        install(RetryAfter)

        defaultRequest {
            accept(ContentType.Application.Json)
            url {
                protocol = URLProtocol.HTTPS
                host = "api.spotify.com"
            }
        }

        install("Authenticator") {
            requestPipeline.intercept(HttpRequestPipeline.State) {
                val currentToken = authToken ?: authenticate()
                context.header(HttpHeaders.Authorization, "Bearer ${currentToken.token}")
            }
        }
    }

    private var authToken: OAuthToken? = null

    private suspend fun authenticate(): OAuthToken =
        accountsService.authenticate(clientKey, clientSecret)

    private suspend fun <T : Any> handle(
        response: HttpResponse,
        targetType: Class<T>
    ): Resource<T> = when (val status = response.status) {
        HttpStatusCode.OK -> {
            val adapter = moshi.adapter(targetType)
            val artist = adapter.fromJson(response.readText())!!
            Resource.Loaded(artist, response.etag())
        }

        HttpStatusCode.NotModified -> Resource.Cached

        HttpStatusCode.NotFound -> Resource.NotFound

        else -> {
            val errorPayload = errorAdapter.fromJson(response.readText())
            if (errorPayload != null) {
                Resource.Failed(errorPayload.status, errorPayload.message)
            } else {
                Resource.Failed(status.value, "An unexpected error occurred.")
            }
        }
    }

    override suspend fun getArtist(id: String): Resource<Artist> {
        require(id.isNotEmpty())
        val response = http.get<HttpResponse>(path = "/v1/artists/$id")
        return handle(response, Artist::class.java)
    }

    override suspend fun getSeveralArtists(ids: List<String>): Resource<List<Artist?>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun getArtistAlbums(artistId: String, limit: Int, offset: Int): Paging<Album> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun getAlbum(id: String): Resource<Album> {
        require(id.isNotEmpty())
        val response = http.get<HttpResponse>(path = "/v1/albums/$id")
        return handle(response, Album::class.java)
    }

    override suspend fun getSeveralAlbums(ids: List<String>): Resource<List<Album?>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun getAlbumTracks(albumId: String, limit: Int, offset: Int): Paging<Track> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun getTrack(id: String): Resource<Track> {
        require(id.isNotEmpty())
        val response = http.get<HttpResponse>(path = "/v1/tracks/$id")
        return handle(response, Track::class.java)
    }

    override suspend fun getSeveralTracks(ids: List<String>): List<Track?> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun getTrackFeatures(trackId: String): Resource<AudioFeatures> {
        require(trackId.isNotEmpty())
        val response = http.get<HttpResponse>(path = "/v1/audio-features/$trackId")
        return handle(response, AudioFeatures::class.java)
    }

    override suspend fun getSeveralTrackFeatures(trackIds: List<String>): Resource<List<AudioFeatures?>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun search(
        query: String,
        type: Set<String>,
        limit: Int,
        offset: Int
    ): SearchResults {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}