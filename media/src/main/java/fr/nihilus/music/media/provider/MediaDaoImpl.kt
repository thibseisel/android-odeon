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

import fr.nihilus.music.common.context.RxSchedulers
import fr.nihilus.music.media.di.ServiceScoped
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.Disposables
import javax.inject.Inject

/**
 * Implementation of [MediaDao] that wraps a [MediaProvider], delegating read/write operations to it
 * while adding a provider layer and thread confinement.
 *
 * @param provider The media provider to which operations should be delegated.
 * @param schedulers The pool of RxJava schedulers on which background operations should be performed.
 */
@ServiceScoped
internal class MediaDaoImpl
@Inject constructor(
    private val provider: MediaProvider,
    private val schedulers: RxSchedulers
) : MediaDao {

    override val tracks: Flowable<List<Track>> = produceUpToDateMediaList(
        MediaProvider.MediaType.TRACKS,
        provider::queryTracks
    )

    override val albums: Flowable<List<Album>> = produceUpToDateMediaList(
        MediaProvider.MediaType.ALBUMS,
        provider::queryAlbums
    )

    override val artists: Flowable<List<Artist>> = produceUpToDateMediaList(
        MediaProvider.MediaType.ARTISTS,
        provider::queryArtists
    )

    override fun deleteTracks(trackIds: LongArray): Completable = Completable.fromAction {
        provider.deleteTracks(trackIds)
    }.subscribeOn(schedulers.Database)

    private fun produceUpdateQueryTrigger(mediaType: MediaProvider.MediaType) = Flowable.create<Unit>({ emitter ->
        val observer = object : MediaProvider.Observer(mediaType) {
            override fun onChanged() {
                // Request to reload the list as it has been updated.
                if (!emitter.isCancelled) {
                    emitter.onNext(Unit)
                }
            }
        }

        if (!emitter.isCancelled) {
            provider.registerObserver(observer)
            emitter.setDisposable(Disposables.fromAction {
                provider.unregisterObserver(observer)
            })
        }

        // Emit the current media list. This behavior is the same as Rooms'.
        if (!emitter.isCancelled) {
            emitter.onNext(Unit)
        }
    }, BackpressureStrategy.LATEST)

    private fun <E> produceUpToDateMediaList(
        mediaType: MediaProvider.MediaType,
        mediaListProvider: () -> List<E>
    ): Flowable<List<E>> = produceUpdateQueryTrigger(mediaType)
        .observeOn(schedulers.Database)
        .flatMapSingle { Single.fromCallable(mediaListProvider) }
}