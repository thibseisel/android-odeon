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

import fr.nihilus.music.core.test.coroutines.flow.NeverFlow
import fr.nihilus.music.core.test.stub
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

internal interface TestDao<M> {
    fun update(updatedList: List<M>)
    fun complete()
    fun failWith(exception: Exception)
}

internal sealed class TestMediaDao<M>(initialList: List<M>?) : MediaDao, TestDao<M> {
    protected val mediaStream: ConflatedBroadcastChannel<List<M>> =
        if (initialList == null) ConflatedBroadcastChannel()
        else ConflatedBroadcastChannel(initialList)

    override val tracks: Flow<List<Track>> get() = NeverFlow
    override val albums: Flow<List<Album>> get() = NeverFlow
    override val artists: Flow<List<Artist>> get() = NeverFlow
    final override suspend fun deleteTracks(trackIds: LongArray): Int = stub()

    override fun update(updatedList: List<M>) {
        mediaStream.offer(updatedList)
    }

    override fun complete() {
        mediaStream.close()
    }

    override fun failWith(exception: Exception) {
        mediaStream.close(exception)
    }
}

internal class TestTrackDao(initialTrackList: List<Track>? = null) : TestMediaDao<Track>(initialTrackList) {
    override val tracks: Flow<List<Track>> get() = mediaStream.asFlow()
}

internal class TestAlbumDao(initialAlbumList: List<Album>? = null) : TestMediaDao<Album>(initialAlbumList) {
    override val albums: Flow<List<Album>> get() = mediaStream.asFlow()
}

internal class TestArtistDao(initialArtistList: List<Artist>? = null) : TestMediaDao<Artist>(initialArtistList) {
    override val artists: Flow<List<Artist>> get() = mediaStream.asFlow()
}