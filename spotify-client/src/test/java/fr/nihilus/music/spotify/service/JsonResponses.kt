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

import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import org.intellij.lang.annotations.Language
import java.io.BufferedReader
import java.io.FileNotFoundException

private object TestResources {

    fun getTextFromFile(filepath: String): String {
        val classLoader = this.javaClass.classLoader
            ?: error("${this.javaClass} has no ClassLoader")
        val resourceFileInput = classLoader.getResourceAsStream(filepath)
            ?: throw FileNotFoundException(filepath)
        return resourceFileInput.bufferedReader().use(BufferedReader::readText)
    }
}

/**
 * Create an HTTP response with the provided [json] as the content body.
 *
 * @param json The content of the response, formatted as a valid JSON string.
 * @param status The status code of the HTTP response. Defaults to 200 (OK).
 */
internal fun respondJson(
    @Language("JSON") json: String,
    status: HttpStatusCode = HttpStatusCode.OK
) = respond(json, status, headersOf(
        HttpHeaders.ContentType,
        ContentType.Application.Json.toString()
    ))

/**
 * Create an HTTP response whose content body is read from a resource file at the given [filepath].
 *
 * @param filepath Path of the file relative to the root resource folder.
 * @param status The status code of the HTTP response. Defaults to 200 (OK).
 */
internal fun respondFile(
    filepath: String,
    status: HttpStatusCode = HttpStatusCode.OK
) = respond(TestResources.getTextFromFile(filepath), status)

/**
 * Produces the JSON response body returned by the Spotify Web API when it returns a 400+ status code.
 *
 * @param status The status associated with the response.
 * @param message The error message provided as the `error` property of the JSON response.
 */
@Language("JSON")
internal fun jsonApiError(status: HttpStatusCode, message: String): String = """{
    "error": {
      "status": ${status.value},
      "message": "$message"
    }
}""".trimIndent()

internal const val TEST_TOKEN_STRING = "MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"