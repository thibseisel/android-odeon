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
import fr.nihilus.music.spotify.model.SpotifyError
import okhttp3.Interceptor
import okhttp3.Response

class SpotifyErrorInterceptor(
    private val moshi: Moshi
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // Execute the request.
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.isSuccessful) {
            return response
        }

        val adapter = moshi.adapter(SpotifyError::class.java)
        val errorBody = response.body()?.string() ?: error("Response should have a body")
        val error = adapter.fromJson(errorBody)

        when (val status = response.code()) {
            else -> TODO("Throw corresponding exception for each interesting status code.")
        }
    }
}