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

import android.util.Base64
import fr.nihilus.music.spotify.HttpStatus
import fr.nihilus.music.spotify.OAuthToken
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * An [Interceptor] that automatically performs token authentication to the Spotify API.
 *
 * @param authService The remote service that produces tokens.
 * @param clientKey the Spotify client key registered for this application.
 * @param clientSecret The Spotify secret key used to authenticate with the client key.
 */
internal class SpotifyAuthentication(
    private val authService: SpotifyAccountsService,
    clientKey: String,
    clientSecret: String
): Interceptor {
    private val base64Key = buildApiKey(clientKey, clientSecret)
    @Volatile private var token: OAuthToken? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        if (token == null) {
            // Make sure that only one concurrent request is made to obtain an access token.
            val (token, _, _) = authenticate()

            // Update the original request to use the newly generated token.
            val originalRequest = chain.request()
            return chain.proceed(originalRequest.authenticatedWith(token))
        } else {
            val originalRequest = chain.request()
            val authenticatedRequest = originalRequest.authenticatedWith(token!!.token)
            val response = chain.proceed(authenticatedRequest)

            return if (response.code() != HttpStatus.UNAUTHORIZED) {
                // The currently saved token is still valid.
                response
            } else {
                // The currently saved token has expired.
                // Renew it then re-attempt the request.
                response.close()
                val (renewedToken, _, _) = authenticate()
                val renewedTokenRequest = originalRequest.authenticatedWith(renewedToken)
                chain.proceed(renewedTokenRequest)
            }
        }
    }

    private fun buildApiKey(clientKey: String, clientSecret: String): String {
        val compositeKey = "$clientKey:$clientSecret".toByteArray(Charsets.UTF_8)
        return Base64.encodeToString(compositeKey, Base64.DEFAULT)
    }

    private fun authenticate(): OAuthToken = synchronized(this) {
        authService.authenticate(base64Key).also { token = it }
    }

    private fun Request.authenticatedWith(token: String): Request = newBuilder()
        .header("Authorization", "Bearer $token")
        .build()
}