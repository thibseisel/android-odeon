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

import io.ktor.utils.io.errors.IOException

/**
 * Thrown when the Spotify API responds with an unexpected HTTP status code and the client is unable to recover.
 * @param status The status code associated with the HTTP response.
 * @param description The optional error message provided by the API response, if any.
 */
internal class ApiException(
    val status: Int,
    val description: String?
) : IOException("Unexpected HTTP status $status: $description")