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

import fr.nihilus.music.spotify.OAuthToken
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

internal abstract class SpotifyAccountsService {

    @POST("token")
    @FormUrlEncoded
    abstract fun authenticateInternal(
        @Field("grant_type") grantType: String,
        @Header("Authorization") authorization: String
    ): OAuthToken

    fun authenticate(base64key: String): OAuthToken =
        authenticateInternal("client_credentials", "Basic $base64key")
}