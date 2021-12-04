/*
 * Copyright 2020 Thibault Seisel
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

import io.ktor.client.HttpClient
import io.ktor.client.features.HttpClientFeature
import io.ktor.client.features.HttpSend
import io.ktor.client.features.feature
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.takeFrom
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.util.AttributeKey
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * [HttpClient] feature that automatically re-sends requests if the server responded with
 * [the HTTP 429 status code][HttpStatusCode.TooManyRequests]. Requests are retried after
 * a given delay defined by the value of the [Retry-After header][HttpHeaders.RetryAfter].
 */
internal object RetryAfter : HttpClientFeature<Unit, RetryAfter> {
    override val key: AttributeKey<RetryAfter> = AttributeKey("RetryAfter")

    override fun prepare(block: Unit.() -> Unit): RetryAfter = this

    override fun install(feature: RetryAfter, scope: HttpClient) {
        // Add an interceptor to the send pipeline to transform the original call.
        scope.feature(HttpSend)?.intercept { original, _ ->
            val status = original.response.status
            val retryAfterSeconds = original.response.headers[HttpHeaders.RetryAfter]?.toIntOrNull()

            if (status != HttpStatusCode.TooManyRequests || retryAfterSeconds == null) original else {
                // We received the 429 status code: wait and re-issue the request.
                Timber.tag("TooManyRequests").d(
                    "Need to wait %s seconds before retrying request: %s",
                    retryAfterSeconds,
                    original.request.url
                )

                // Since the number of seconds to wait is rounded down,
                // we need to wait one more second to make sure the delay is elapsed.
                delay((retryAfterSeconds + 1) * 1000L)

                val reattemptedRequest = HttpRequestBuilder()
                reattemptedRequest.takeFrom(original.request)
                execute(reattemptedRequest)
            }

        } ?: error("HttpSend is an internal feature that should always be installed.")
    }
}