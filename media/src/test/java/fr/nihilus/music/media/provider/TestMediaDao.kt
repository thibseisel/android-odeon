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

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.FlowableProcessor

interface TestDao<M> {
    fun update(updatedList: List<M>)
    fun complete()
    fun failWith(exception: Exception)
}

internal sealed class TestMediaDao<M>(initialList: List<M>?) : RxMediaDao, TestDao<M> {
    protected val mediaStream: FlowableProcessor<List<M>> =
        if (initialList == null) BehaviorProcessor.create()
        else BehaviorProcessor.createDefault(initialList)

    override val tracks: Flowable<List<Track>> get() = Flowable.never()
    override val albums: Flowable<List<Album>> get() = Flowable.never()
    override val artists: Flowable<List<Artist>> get() = Flowable.never()

    final override fun deleteTracks(trackIds: LongArray) = Completable.complete()

    override fun update(updatedList: List<M>) {
        mediaStream.onNext(updatedList)
    }

    override fun complete() {
        mediaStream.onComplete()
    }

    override fun failWith(exception: Exception) {
        mediaStream.onError(exception)
    }
}

internal class TestTrackDao(initialTrackList: List<Track>? = null) : TestMediaDao<Track>(initialTrackList) {
    override val tracks: Flowable<List<Track>> get() = mediaStream.onBackpressureBuffer()
}

internal class TestAlbumDao(initialAlbumList: List<Album>? = null) : TestMediaDao<Album>(initialAlbumList) {
    override val albums: Flowable<List<Album>> get() = mediaStream.onBackpressureBuffer()
}

internal class TestArtistDao(initialArtistList: List<Artist>? = null) : TestMediaDao<Artist>(initialArtistList) {
    override val artists: Flowable<List<Artist>> get() = mediaStream.onBackpressureBuffer()
}