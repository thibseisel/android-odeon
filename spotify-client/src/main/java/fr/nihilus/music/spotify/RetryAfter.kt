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

package fr.nihilus.music.spotify

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.*

/**
 * [HttpClient] feature that automatically re-sends requests
 * if the server responded with [the HTTP 421 status code][HttpStatusCode.TooManyRequests].
 * Requests are retried after a given delay defined by the value of the [Retry-After header][HttpHeaders.RetryAfter].
 */
internal object RetryAfter : HttpClientFeature<Unit, RetryAfter> {
    override val key: AttributeKey<RetryAfter> = AttributeKey("RetryAfter")

    override fun prepare(block: Unit.() -> Unit): RetryAfter = this

    override fun install(feature: RetryAfter, scope: HttpClient) {
        // Add an interceptor to the send pipeline to transform the original call.
        scope.feature(HttpSend)?.intercept { original ->
            val status = original.response.status
            val retryAfterSeconds = original.response.headers[HttpHeaders.RetryAfter]?.toIntOrNull()

            if (status != HttpStatusCode.TooManyRequests || retryAfterSeconds == null) original else {
                // We received the 429 status code: close the original response, wait and re-issue the request.
                original.close()
                delay(retryAfterSeconds * 1000L)

                val reattemptedRequest = HttpRequestBuilder()
                reattemptedRequest.takeFrom(original.request)
                execute(reattemptedRequest)
            }

        } ?: error("HttpSend is an internal feature that should always be installed.")
    }
}