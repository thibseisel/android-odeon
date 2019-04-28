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

package fr.nihilus.music.media.playlists

import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.processors.BehaviorProcessor

internal class TestPlaylistDao(
    initialPlaylists: List<Playlist>? = null
) : PlaylistDao {

    private val _playlists =
        if (initialPlaylists != null) BehaviorProcessor.createDefault(initialPlaylists)
        else BehaviorProcessor.create()

    override val playlistsFlow: Flowable<List<Playlist>>
        get() = _playlists.onBackpressureBuffer()

    override fun getPlaylists(): Single<List<Playlist>> = _playlists.singleOrError()

    override fun savePlaylist(playlist: Playlist): Long = 0L

    override fun addTracks(tracks: Iterable<PlaylistTrack>) {
        // Not required for tests.
    }

    override fun deletePlaylist(playlistId: Long) {
        // Not required for tests.
    }

    override fun getPlaylistTracks(playlistId: Long): Single<List<PlaylistTrack>> {
        // TODO Implement playlist content
        return Single.just(emptyList())
    }

    override fun getPlaylistsHavingTracks(trackIds: LongArray): Single<LongArray> {
        // TODO Implement playlist content
        return Single.just(LongArray(0))
    }

    override fun deletePlaylistTracks(trackIds: LongArray) = Unit

    fun updatePlaylists(newPlaylists: List<Playlist>?) =
        if (newPlaylists != null) _playlists.onNext(newPlaylists)
        else _playlists.onComplete()
}