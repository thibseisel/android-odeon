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

internal sealed class Resource<out T : Any> {

    data class Loaded<T : Any>(
        val data: T,
        val eTag: String?
    ) : Resource<T>()

    object Cached : Resource<Nothing>()

    object NotFound : Resource<Nothing>()

    data class Failed(
        val status: Int,
        val message: String?
    ) : Resource<Nothing>()
}