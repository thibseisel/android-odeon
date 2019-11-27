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

package fr.nihilus.music.media.provider

import fr.nihilus.music.media.dagger.ServiceScoped
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

/**
 * Implementation of [MediaDao] that wraps a [MediaProvider] to expose a Reactive API.
 *
 * @param provider The media provider to which operations should be delegated.
 */
@ServiceScoped
internal class MediaDaoImpl
@Inject constructor(
    private val provider: MediaProvider
) : MediaDao {

    override val tracks: Flow<List<Track>> = produceUpToDateMediaList(
        MediaProvider.MediaType.TRACKS,
        provider::queryTracks
    )

    override val albums: Flow<List<Album>> = produceUpToDateMediaList(
        MediaProvider.MediaType.ALBUMS,
        provider::queryAlbums
    )

    override val artists: Flow<List<Artist>> = produceUpToDateMediaList(
        MediaProvider.MediaType.ARTISTS,
        provider::queryArtists
    )

    override suspend fun deleteTracks(trackIds: LongArray): Int = provider.deleteTracks(trackIds)

    private fun produceUpdateQueryTrigger(mediaType: MediaProvider.MediaType) = callbackFlow {
        // Emit the current media list.
        offer(Unit)

        // Register an observer that request to reload the list when it has changed.
        val observer = object : MediaProvider.Observer(mediaType) {
            override fun onChanged() {
                offer(Unit)
            }
        }

        provider.registerObserver(observer)
        awaitClose { provider.unregisterObserver(observer) }
    }.conflate()

    private inline fun <E> produceUpToDateMediaList(
        mediaType: MediaProvider.MediaType,
        crossinline mediaListProvider: suspend () -> List<E>
    ): Flow<List<E>> = produceUpdateQueryTrigger(mediaType).mapLatest {
        mediaListProvider()
    }
}