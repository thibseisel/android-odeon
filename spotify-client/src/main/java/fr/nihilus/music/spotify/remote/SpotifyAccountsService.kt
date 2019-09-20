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

import android.util.Base64
import com.squareup.moshi.Moshi
import fr.nihilus.music.spotify.OAuthToken
import fr.nihilus.music.spotify.isSuccessful
import fr.nihilus.music.spotify.model.AuthenticationError
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.features.UserAgent
import io.ktor.client.features.defaultRequest
import io.ktor.client.request.accept
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.URLProtocol
import java.lang.Exception

internal interface SpotifyAccountsService {

    /**
     * Create an access token for the given [clientId] and keep it until it is expired.
     * Calls to other APIs of this class will use the generated token.
     *
     * @param clientId The public identifier of the client application.
     * This identifier is generated by Spotify on the developer dashboard.
     * @param clientSecret The secret key for the specified client.
     *
     * @return The token that has been generated.
     */
    suspend fun authenticate(clientId: String, clientSecret: String): OAuthToken
}

internal class SpotifyAccountsServiceImpl(
    engine: HttpClientEngine,
    moshi: Moshi,
    userAgent: String
) : SpotifyAccountsService {

    private val tokenAdapter = moshi.adapter(OAuthToken::class.java)
    private val errorAdapter = moshi.adapter(AuthenticationError::class.java)

    private val http = HttpClient(engine) {
        expectSuccess = false
        install(UserAgent) {
            agent = userAgent
        }

        defaultRequest {
            accept(ContentType.Application.Json)
            url {
                protocol = URLProtocol.HTTPS
                host = "accounts.spotify.com"
            }
        }
    }

    override suspend fun authenticate(clientId: String, clientSecret: String): OAuthToken {
        val compositeKey = "$clientId:$clientSecret".toByteArray()
        val base64Key =
            Base64.encodeToString(compositeKey, Base64.NO_WRAP)

        val response = http.post<HttpResponse>(path = "api/token") {
            header(HttpHeaders.Authorization, "Basic $base64Key")
            body =
                FormDataContent(Parameters.build {
                    append("grant_type", "client_credentials")
                })
        }

        return if (response.isSuccessful) {
            tokenAdapter.fromJson(response.readText())!!
        } else {
            val errorPayload = errorAdapter.fromJson(response.readText())!!
            throw AuthenticationException(
                errorPayload.error,
                errorPayload.description
            )
        }
    }
}

internal class AuthenticationException(
    val error: String,
    val description: String?
) : Exception("$error ($description)")
