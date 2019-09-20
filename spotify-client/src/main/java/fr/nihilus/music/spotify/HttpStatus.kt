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

/**
 * Constants for HTTP statuses that could be returned by the Spotify API.
 */
@Deprecated("Ktor provides an exhaustive list of HTTP statuses.")
internal object HttpStatus {
    /**
     * OK - The request has succeeded.
     * The client can read the result of the request in the body and the headers of the response.
     */
    const val OK = 200

    /**
     * Created - The request has been fulfilled and resulted in a new resource being created.
     */
    const val CREATED = 201

    /**
     * Accepted - The request has been accepted for processing,
     * but the processing has not been completed.
     */
    const val ACCEPTED = 202

    /**
     * No Content - The request has succeeded but returns no message body.
     */
    const val NO_CONTENT = 204

    /**
     * Not Modified.
     */
    const val NOT_MODIFIED = 304

    /**
     * Bad Request - The request could not be understood by the server due to malformed syntax.
     * The message body will contain more information.
     */
    const val BAD_REQUEST = 400

    /**
     * Unauthorized - The request requires user authentication or,
     * if the request included authorization credentials,
     * authorization has been refused for those credentials.
     */
    const val UNAUTHORIZED = 401

    /**
     * Forbidden - The server understood the request, but is refusing to fulfill it.
     */
    const val FORBIDDEN = 403

    /**
     * Not Found - The requested resource does not exist or is no longer available.
     */
    const val NOT_FOUND = 404

    /**
     * Too Many Requests - Rate limiting has been applied.
     * Rate Limiting enables Web API to share access bandwidth to its resources equally across all users.
     * Rate limiting is applied as per application based on Client ID,
     * and regardless of the number of users who use the application simultaneously.
     *
     * When this happens, check the Retry-After header, where you will see a number displayed.
     * This is the number of seconds that you need to wait, before you try your request again.
     */
    const val TOO_MANY_REQUESTS = 429

    /**
     * Internal Server Error.
     */
    const val INTERNAL_SERVER_ERROR = 500

    /**
     * Bad Gateway - The server was acting as a gateway or proxy
     * and received an invalid response from the upstream server.
     */
    const val BAD_GATEWAY = 502

    /**
     * Service Unavailable - The server is currently unable to handle the request
     * due to a temporary condition which will be alleviated after some delay.
     * You can choose to resend the request again.
     */
    const val SERVICE_UNAVAILABLE = 503
}