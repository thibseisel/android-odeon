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

import fr.nihilus.music.core.os.PermissionDeniedException
import fr.nihilus.music.media.provider.MediaProvider.MediaType
import io.kotlintest.inspectors.forNone
import io.kotlintest.inspectors.forOne
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

internal class MediaDaoTest {

    @Test
    fun givenDeniedPermission_whenSubscribingTracks_thenTerminateWithPermissionDeniedException() {
        shouldTerminateIfPermissionIsDenied(MediaDao::tracks)
    }

    @Test
    fun givenDeniedPermission_whenSubscribingAlbums_thenTerminateWithPermissionDeniedException() {
        shouldTerminateIfPermissionIsDenied(MediaDao::albums)
    }

    @Test
    fun givenDeniedPermission_whenSubscribingArtists_thenTerminateWithPermissionDeniedException() {
        shouldTerminateIfPermissionIsDenied(MediaDao::artists)
    }

    @Test
    fun givenTrackSubscription_whenReceivingUpdateAndPermissionIsRevoked_thenTerminateWithPermissionDeniedException() {
        shouldTerminateWhenPermissionIsRevoked(MediaType.TRACKS, MediaDao::tracks)
    }

    @Test
    fun givenAlbumSubscription_whenReceivingUpdateAndPermissionIsRevoked_thenTerminateWithPermissionDeniedException() {
        shouldTerminateWhenPermissionIsRevoked(MediaType.ALBUMS, MediaDao::albums)
    }

    @Test
    fun givenArtistSubscription_whenReceivingUpdateAndPermissionIsRevoked_thenTerminateWithPermissionDeniedException() {
        shouldTerminateWhenPermissionIsRevoked(MediaType.ARTISTS, MediaDao::artists)
    }

    @Test
    fun givenTrackSubscription_whenTerminatingWithError_thenUnregisterObserver() {
        shouldUnregisterObserverOnFlowFailure(MediaType.TRACKS, MediaDao::tracks)
    }

    @Test
    fun givenAlbumSubscription_whenTerminatingWithError_thenUnregisterObserver() {
        shouldUnregisterObserverOnFlowFailure(MediaType.ALBUMS, MediaDao::albums)
    }

    @Test
    fun givenArtistSubscription_whenTerminatingWithError_thenUnregisterObserver() {
        shouldUnregisterObserverOnFlowFailure(MediaType.ARTISTS, MediaDao::artists)
    }

    @Test
    fun whenSubscribingTracks_thenLoadCurrentTrackList() {
        val mediaDao = MediaDaoImpl(TestMediaProvider(tracks = SAMPLE_TRACKS))
        shouldLoadMediaOnSubscription(mediaDao.tracks, SAMPLE_TRACKS)
    }

    @Test
    fun whenSubscribingAlbums_thenLoadCurrentAlbumList() {
        val mediaDao = MediaDaoImpl(TestMediaProvider(albums = SAMPLE_ALBUMS))
        shouldLoadMediaOnSubscription(mediaDao.albums, SAMPLE_ALBUMS)
    }

    @Test
    fun whenSubscribingArtists_thenLoadCurrentArtistList() {
        val mediaDao = MediaDaoImpl(TestMediaProvider(artists = SAMPLE_ARTISTS))
        shouldLoadMediaOnSubscription(mediaDao.artists, SAMPLE_ARTISTS)
    }

    @Test
    fun givenTrackSubscription_whenNotifiedForChange_thenReloadTrackList() {
        val provider = TestMediaProvider()
        val mediaDao = MediaDaoImpl(provider)
        shouldReloadMediaWhenNotified(provider, MediaType.TRACKS, mediaDao.tracks, SAMPLE_TRACKS)
    }

    @Test
    fun givenAlbumSubscription_whenNotifiedForChange_thenReloadAlbumList() {
        val provider = TestMediaProvider()
        val mediaDao = MediaDaoImpl(provider)
        shouldReloadMediaWhenNotified(provider, MediaType.ALBUMS, mediaDao.albums, SAMPLE_ALBUMS)
    }

    @Test
    fun givenArtistSubscription_whenNotifiedForChange_thenReloadArtistList() {
        val provider = TestMediaProvider()
        val mediaDao = MediaDaoImpl(provider)
        shouldReloadMediaWhenNotified(provider, MediaType.ARTISTS, mediaDao.artists, SAMPLE_ARTISTS)
    }

    @Test
    fun whenSubscribingTracks_thenRegisterAnObserver() {
        shouldRegisterAnObserverWhenSubscribed(MediaType.TRACKS, MediaDao::tracks)
    }

    @Test
    fun whenSubscribingAlbums_thenRegisterAnObserver() {
        shouldRegisterAnObserverWhenSubscribed(MediaType.ALBUMS, MediaDao::albums)
    }

    @Test
    fun whenSubscribingArtists_thenRegisterAnObserver() {
        shouldRegisterAnObserverWhenSubscribed(MediaType.ARTISTS, MediaDao::artists)
    }

    @Test
    fun givenAnActiveTrackSubscription_whenDisposingIt_thenUnregisterObserver() {
        shouldUnregisterObserverOnDisposal(MediaType.TRACKS, MediaDao::tracks)
    }

    @Test
    fun givenAnActiveAlbumSubscription_whenDisposingIt_thenUnregisterObserver() {
        shouldUnregisterObserverOnDisposal(MediaType.ALBUMS, MediaDao::albums)
    }

    @Test
    fun givenAnActiveArtistSubscription_whenDisposingIt_thenUnregisterObserver() {
        shouldUnregisterObserverOnDisposal(MediaType.ARTISTS, MediaDao::artists)
    }

    @Test
    fun givenDeniedPermission_whenDeletingTracks_thenFailWithPermissionDeniedException() = runBlockingTest {
        val provider = TestMediaProvider()
        provider.hasStoragePermission = false
        val mediaDao = MediaDaoImpl(provider)

        shouldThrow<PermissionDeniedException> {
            mediaDao.deleteTracks(longArrayOf(161L))
        }
    }

    private fun shouldRegisterAnObserverWhenSubscribed(
        observerType: MediaType,
        flowProvider: MediaDao.() -> Flow<Any>
    ) = runBlockingTest {
        val provider = TestMediaProvider()
        val mediaDao = MediaDaoImpl(provider)

        val values = mediaDao.flowProvider().produceIn(this)
        try {
            provider.registeredObservers.forOne { it.type shouldBe observerType }
        } finally {
            values.cancel()
        }

    }

    private fun shouldUnregisterObserverOnDisposal(
        observerType: MediaType,
        flowProvider: MediaDao.() -> Flow<Any>
    ) = runBlockingTest {
        val provider = TestMediaProvider()
        val mediaDao = MediaDaoImpl(provider)

        mediaDao.flowProvider().take(1).collect()
        provider.registeredObservers.forNone { it.type shouldBe observerType }
    }

    private fun <M> shouldReloadMediaWhenNotified(
        provider: TestMediaProvider,
        type: MediaType,
        stream: Flow<List<M>>,
        expected: List<M>
    ) = runBlockingTest {
        val updates = stream.produceIn(this)
        try {
            updates.receive()
            provider.notifyChange(type)
            updates.receive() shouldContainExactly expected
        } finally {
            updates.cancel()
        }
    }

    private fun <M> shouldLoadMediaOnSubscription(
        flow: Flow<List<M>>,
        expectedMedia: List<M>
    ) = runBlockingTest {
        val values = flow.take(1).toList()

        values shouldHaveSize 1
        values[0] shouldContainExactly expectedMedia
    }

    private fun shouldUnregisterObserverOnFlowFailure(
        type: MediaType,
        flowProvider: MediaDao.() -> Flow<Any>
    ) = runBlockingTest {
        // Given an active subscription...
        val revokingPermissionProvider = TestMediaProvider()
        val mediaDao = MediaDaoImpl(revokingPermissionProvider)
        val values = mediaDao.flowProvider().produceIn(this)
        values.receive()

        // When receiving an update that triggers an error...
        revokingPermissionProvider.hasStoragePermission = false
        revokingPermissionProvider.notifyChange(type)

        try {
            revokingPermissionProvider.registeredObservers.forNone { it.type shouldBe type }
        } finally {
            values.cancel()
        }
    }

    private fun shouldTerminateIfPermissionIsDenied(
        flowProvider: MediaDao.() -> Flow<Any>
    ) = runBlockingTest {
        val deniedProvider = TestMediaProvider()
        deniedProvider.hasStoragePermission = false

        val mediaDao = MediaDaoImpl(deniedProvider)

        shouldThrow<PermissionDeniedException> {
            mediaDao.flowProvider().take(1).collect()
        }
    }

    private fun shouldTerminateWhenPermissionIsRevoked(
        type: MediaType,
        flowProvider: MediaDao.() -> Flow<Any>
    ) = runBlockingTest {
        // Given an active subscription...
        val revokingPermissionProvider = TestMediaProvider()
        val mediaDao = MediaDaoImpl(revokingPermissionProvider)
        val updates = mediaDao.flowProvider().produceIn(this)
        updates.receive()

        try {
            // When permission is revoked and an update notification is received...
            revokingPermissionProvider.hasStoragePermission = false
            revokingPermissionProvider.notifyChange(type)

            // Flow should fail with an error.
            shouldThrow<PermissionDeniedException> {
                updates.receive()
            }

        } finally {
            updates.cancel()
        }
    }
}