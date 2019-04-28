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

import android.Manifest
import fr.nihilus.music.media.permissions.PermissionDeniedException
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.FlowableProcessor

internal class TestMediaDao(
    initialTracks: List<Track>? = null,
    initialAlbums: List<Album>? = null,
    initialArtists: List<Artist>? = null
) : RxMediaDao {
    private val _tracks = processorOf(initialTracks)
    private val _albums = processorOf(initialAlbums)
    private val _artists = processorOf(initialArtists)

    var hasStoragePermission = true

    override val tracks: Flowable<List<Track>> get() = _tracks.onBackpressureBuffer()
    override val albums: Flowable<List<Album>> get() = _albums.onBackpressureBuffer()
    override val artists: Flowable<List<Artist>> get() = _artists.onBackpressureBuffer()

    fun updateTracks(newTracks: List<Track>?) = update(_tracks, newTracks)
    fun updateAlbums(newAlbums: List<Album>?) = update(_albums, newAlbums)
    fun updateArtists(newArtists: List<Artist>?) = update(_artists, newArtists)

    private fun <M> update(processor: FlowableProcessor<List<M>>, updated: List<M>?) = when {
        !hasStoragePermission -> processor.onError(PermissionDeniedException(Manifest.permission.READ_EXTERNAL_STORAGE))
        updated == null -> _tracks.onComplete()
        else -> processor.onNext(updated)
    }

    override fun deleteTracks(trackIds: LongArray): Completable = Completable.fromAction {
        if (!hasStoragePermission) throw PermissionDeniedException(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private fun <E> processorOf(initialValue: List<E>?) =
        if (initialValue != null) BehaviorProcessor.createDefault(initialValue)
        else BehaviorProcessor.create()
}

internal sealed class TestDao<M>(initialList: List<M>?) : RxMediaDao {
    protected val mediaStream: FlowableProcessor<List<M>> =
        if (initialList == null) BehaviorProcessor.create() else BehaviorProcessor.createDefault(initialList)

    final override fun deleteTracks(trackIds: LongArray) = Completable.complete()

    fun update(updatedList: List<M>) {
        mediaStream.onNext(updatedList)
    }

    fun complete() {
        mediaStream.onComplete()
    }

    fun failWith(exception: Exception) {
        mediaStream.onError(exception)
    }
}

internal class TrackDao(initialTrackList: List<Track>? = null) : TestDao<Track>(initialTrackList) {
    override val tracks: Flowable<List<Track>> get() = mediaStream.onBackpressureBuffer()
    override val albums: Flowable<List<Album>> get() = Flowable.never()
    override val artists: Flowable<List<Artist>> get() = Flowable.never()
}

internal class AlbumDao(initialAlbumList: List<Album>? = null) : TestDao<Album>(initialAlbumList) {
    override val tracks: Flowable<List<Track>> get() = Flowable.never()
    override val albums: Flowable<List<Album>> get() = mediaStream.onBackpressureBuffer()
    override val artists: Flowable<List<Artist>> get() = Flowable.never()
}

internal class ArtistDao(initialArtistList: List<Artist>? = null) : TestDao<Artist>(initialArtistList) {
    override val tracks: Flowable<List<Track>> get() = Flowable.never()
    override val albums: Flowable<List<Album>> get() = Flowable.never()
    override val artists: Flowable<List<Artist>> get() = mediaStream.onBackpressureBuffer()
}