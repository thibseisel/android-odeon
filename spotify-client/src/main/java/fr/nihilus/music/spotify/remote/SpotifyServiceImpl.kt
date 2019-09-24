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

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import fr.nihilus.music.spotify.OAuthToken
import fr.nihilus.music.spotify.RetryAfter
import fr.nihilus.music.spotify.WrappedJsonAdapter
import fr.nihilus.music.spotify.model.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.http.*
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.io.errors.IOException
import org.jetbrains.annotations.TestOnly
import javax.inject.Inject
import javax.inject.Named

@UseExperimental(KtorExperimentalAPI::class)
internal class SpotifyServiceImpl
@TestOnly constructor(
    engine: HttpClientEngine,
    moshi: Moshi,
    private val accountsService: SpotifyAccountsService,
    userAgent: String,
    private val clientKey: String,
    private val clientSecret: String,
    private var token: OAuthToken? = null
): SpotifyService {

    @Inject constructor(
        engine: HttpClientEngine,
        moshi: Moshi,
        accountsService: SpotifyAccountsService,
        @Named("APP_USER_AGENT") userAgent: String,
        @Named("SPOTIFY_CLIENT_KEY") clientKey: String,
        @Named("SPOTIFY_CLIENT_SECRET") clientSecret: String
    ) : this(engine, moshi, accountsService, userAgent, clientKey, clientSecret, null)

    private val deserializer = moshi.newBuilder()
        .add(MusicalMode::class.java, MusicalModeAdapter())
        .add(Pitch::class.java, PitchAdapter())
        .build()

    private val errorAdapter: JsonAdapter<SpotifyError> = WrappedJsonAdapter(
        "error",
        deserializer.adapter(SpotifyError::class.java)
    )

    private val artistListAdapter = WrappedJsonAdapter("artists", listAdapterOf<Artist>())
    private val albumListAdapter = WrappedJsonAdapter("albums", listAdapterOf<Album>())
    private val trackListAdapter = WrappedJsonAdapter("tracks", listAdapterOf<Track>())
    private val featureListAdapter = WrappedJsonAdapter("audio_features", listAdapterOf<AudioFeatures>())

    private val http = HttpClient(engine) {
        expectSuccess = false

        install(UserAgent) {
            agent = userAgent
        }

        install(RetryAfter)

        install(DefaultRequest) {
            accept(ContentType.Application.Json)
            url {
                protocol = URLProtocol.HTTPS
                host = "api.spotify.com"
            }
        }

        install("AutoTokenAuthentication") {
            // Perform initial authentication if required, then set token has Authorization.
            requestPipeline.intercept(HttpRequestPipeline.State) { _ ->
                val currentToken = token ?: authenticate()
                context.header(HttpHeaders.Authorization, "Bearer ${currentToken.token}")
            }

            // If token is expired, then renew it then re-attempt the request.
            feature(HttpSend)!!.intercept { origin ->
                if (origin.response.status != HttpStatusCode.Unauthorized) origin else {
                    val newToken = authenticate()
                    val request = HttpRequestBuilder()
                    request.takeFrom(origin.request)
                    request.headers[HttpHeaders.Authorization] = "Bearer ${newToken.token}"

                    origin.close()
                    execute(request)
                }
            }
        }
    }

    private suspend fun authenticate(): OAuthToken =
        accountsService.authenticate(clientKey, clientSecret).also { token = it }

    override suspend fun getArtist(id: String): Resource<Artist> {
        require(id.isNotEmpty())
        val response = http.get<HttpResponse>(path = "/v1/artists/$id")
        return singleResource(response, Artist::class.java)
    }

    override suspend fun getSeveralArtists(ids: List<String>): Resource<List<Artist?>> {
        require(ids.size in 0..50)
        val response = http.get<HttpResponse>(path = "/v1/artists") {
            parameter(SpotifyService.QUERY_IDS, ids.joinToString(","))
        }

        return listResource(response, artistListAdapter)
    }

    override fun getArtistAlbums(artistId: String): Flow<Album> {
        val albumPageRequest = HttpRequestBuilder(path = "/v1/artists/$artistId/albums") {
            parameters[SpotifyService.QUERY_INCLUDE_GROUPS] = "album,single"
        }

        return paginatedFlow(albumPageRequest, pagingAdapterOf())
    }

    override suspend fun getAlbum(id: String): Resource<Album> {
        require(id.isNotEmpty())
        val response = http.get<HttpResponse>(path = "/v1/albums/$id")
        return singleResource(response, Album::class.java)
    }

    override suspend fun getSeveralAlbums(ids: List<String>): Resource<List<Album?>> {
        require(ids.size in 0..20)
        val response = http.get<HttpResponse>(path = "/v1/albums") {
            parameter(SpotifyService.QUERY_IDS, ids.joinToString(","))
        }

        return listResource(response, albumListAdapter)
    }

    override fun getAlbumTracks(albumId: String): Flow<Track> {
        val trackPageRequest = HttpRequestBuilder(path = "/v1/albums/$albumId/tracks")
        return paginatedFlow(trackPageRequest, pagingAdapterOf())
    }

    override suspend fun getTrack(id: String): Resource<Track> {
        require(id.isNotEmpty())
        val response = http.get<HttpResponse>(path = "/v1/tracks/$id")
        return singleResource(response, Track::class.java)
    }

    override suspend fun getSeveralTracks(ids: List<String>): Resource<List<Track?>> {
        require(ids.size in 0..50)
        val response = http.get<HttpResponse>(path = "/v1/tracks") {
            parameter(SpotifyService.QUERY_IDS, ids.joinToString(","))
        }

        return listResource(response, trackListAdapter)
    }

    override suspend fun getTrackFeatures(trackId: String): Resource<AudioFeatures> {
        require(trackId.isNotEmpty())
        val response = http.get<HttpResponse>(path = "/v1/audio-features/$trackId")
        return singleResource(response, AudioFeatures::class.java)
    }

    override suspend fun getSeveralTrackFeatures(trackIds: List<String>): Resource<List<AudioFeatures?>> {
        require(trackIds.size in 0..100)
        val response = http.get<HttpResponse>(path = "/v1/audio-features") {
            parameter(SpotifyService.QUERY_IDS, trackIds.joinToString(","))
        }

        return listResource(response, featureListAdapter)
    }

    override fun search(
        query: String,
        type: Set<String>,
        limit: Int,
        offset: Int
    ): SearchResults {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private suspend fun <T : Any> singleResource(
        response: HttpResponse,
        targetType: Class<T>
    ): Resource<T> = when (response.status) {

        HttpStatusCode.OK -> {
            val adapter = deserializer.adapter(targetType)
            val item = adapter.fromJson(response.readText())!!
            Resource.Loaded(item, response.etag())
        }

        HttpStatusCode.NotModified -> Resource.Cached
        HttpStatusCode.NotFound -> Resource.NotFound
        else -> parseApiError(response)
    }

    private suspend fun <T : Any> listResource(
        response: HttpResponse,
        adapter: WrappedJsonAdapter<List<T>>
    ) : Resource<List<T?>> = when (response.status) {

        HttpStatusCode.OK -> {
            val list = adapter.fromJson(response.readText())!!
            Resource.Loaded(list, response.etag())
        }

        HttpStatusCode.NotModified -> Resource.Cached
        else -> parseApiError(response)
    }

    private fun <T> paginatedFlow(
        pageRequest: HttpRequestBuilder,
        pagingAdapter: JsonAdapter<Paging<T>>
    ): Flow<T> = flow {
        var hasNextPage: Boolean
        pagination@ do {
            val response = http.get<HttpResponse>(pageRequest)
            when (response.status) {

                HttpStatusCode.OK -> {
                    // Emit items from the fetched page and prepare to fetch the next one, if any.
                    val pageOfResults = pagingAdapter.fromJson(response.readText())!!
                    for (result in pageOfResults.items) {
                        emit(result)
                    }

                    if (pageOfResults.next != null) {
                        pageRequest.url(pageOfResults.next)
                        hasNextPage = true
                    } else {
                        hasNextPage = false
                    }
                }

                HttpStatusCode.NotFound -> {
                    // The parent resource does not exist. End the flow without an element.
                    break@pagination
                }

                else -> {
                    // That's an unexpected error code.
                    val errorPayload = errorAdapter.fromJson(response.readText())
                    if (errorPayload != null) {
                        throw ApiException(errorPayload.status, errorPayload.message)
                    } else {
                        throw IOException("Unexpected HTTP status ${response.status.value}")
                    }
                }
            }

        } while (hasNextPage)
    }

    private suspend fun parseApiError(response: HttpResponse): Resource.Failed {
        val errorPayload = errorAdapter.fromJson(response.readText())
        return if (errorPayload != null) {
            Resource.Failed(errorPayload.status, errorPayload.message)
        } else {
            Resource.Failed(response.status.value, "An unexpected error occurred.")
        }
    }

    private inline fun <reified T : Any> listAdapterOf(): JsonAdapter<List<T>> {
        val listType = Types.newParameterizedType(List::class.java, T::class.java)
        return deserializer.adapter(listType)
    }

    private inline fun <reified T : Any> pagingAdapterOf(): JsonAdapter<Paging<T>> {
        val pagingType = Types.newParameterizedType(Paging::class.java, T::class.java)
        return deserializer.adapter(pagingType)
    }
}