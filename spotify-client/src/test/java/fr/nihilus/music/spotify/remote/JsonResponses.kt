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

import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import org.intellij.lang.annotations.Language

/**
 * Create an HTTP response with the provided [json] as the content body.
 *
 * @param json The content of the response, formatted as a valid JSON string.
 * @param status The status code of the HTTP response. Defaults to 200 (OK).
 */
internal fun respondJson(
    @Language("JSON") json: String,
    status: HttpStatusCode = HttpStatusCode.OK
) = respond(
    json,
    status,
    headersOf(
        HttpHeaders.ContentType,
        ContentType.Application.Json.toString()
    )
)

/**
 * Produces the JSON response body returned by the Spotify Web API when it returns a 400+ status code.
 *
 * @param status The status associated with the response.
 * @param message The error message provided as the `error` property of the JSON response.
 */
@Language("JSON")
fun jsonApiError(status: HttpStatusCode, message: String): String = """{
    "error": {
      "status": ${status.value},
      "message": "$message"
    }
}""".trimIndent()