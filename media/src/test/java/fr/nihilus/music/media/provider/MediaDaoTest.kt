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

import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import fr.nihilus.music.core.os.PermissionDeniedException
import fr.nihilus.music.media.provider.MediaProvider.MediaType
import fr.nihilus.music.media.provider.MediaProvider.Observer
import io.kotlintest.shouldThrow
import io.reactivex.Flowable
import io.reactivex.schedulers.TestScheduler
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import java.util.concurrent.TimeUnit

internal class MediaDaoTest {

    private val testScheduler = TestScheduler()

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
        shouldUnregisterObserverOnStreamFailure(MediaType.TRACKS, MediaDao::tracks)
    }

    @Test
    fun givenAlbumSubscription_whenTerminatingWithError_thenUnregisterObserver() {
        shouldUnregisterObserverOnStreamFailure(MediaType.ALBUMS, MediaDao::albums)
    }

    @Test
    fun givenArtistSubscription_whenTerminatingWithError_thenUnregisterObserver() {
        shouldUnregisterObserverOnStreamFailure(MediaType.ARTISTS, MediaDao::artists)
    }

    @Test
    fun whenSubscribingTracks_thenStreamShouldNeverComplete() {
        shouldNeverComplete(MediaDao::tracks)
    }

    @Test
    fun whenSubscribingAlbums_thenStreamShouldNeverComplete() {
        shouldNeverComplete(MediaDao::albums)
    }

    @Test
    fun whenSubscribingArtists_thenStreamShouldNeverComplete() {
        shouldNeverComplete(MediaDao::artists)
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

    private fun shouldUnregisterObserverOnDisposal(
        observerType: MediaType,
        streamProvider: MediaDao.() -> Flowable<out Any>
    ) {
        val provider = TestMediaProvider()
        val mediaDao = MediaDaoImpl(provider)

        val subscriber = mediaDao.streamProvider().test()
        testScheduler.triggerActions()
        subscriber.dispose()

        assertThat(provider.registeredObservers).comparingElementsUsing(THEIR_MEDIA_TYPE)
            .doesNotContain(observerType)
    }

    private fun shouldRegisterAnObserverWhenSubscribed(
        observerType: MediaType,
        streamProvider: MediaDao.() -> Flowable<out Any>
    ) {
        val provider = TestMediaProvider()
        val mediaDao = MediaDaoImpl(provider)

        mediaDao.streamProvider().test()
        testScheduler.triggerActions()

        assertThat(provider.registeredObservers).comparingElementsUsing(THEIR_MEDIA_TYPE)
            .contains(observerType)
    }

    private fun <M> shouldReloadMediaWhenNotified(
        provider: TestMediaProvider,
        type: MediaType,
        stream: Flowable<List<M>>,
        expected: List<M>
    ) {
        val subscriber = stream.test()
        subscriber.awaitCount(1)

        provider.notifyChange(type)
        testScheduler.triggerActions()

        subscriber.assertValueAt(1, expected)
    }

    private fun <M> shouldLoadMediaOnSubscription(stream: Flowable<List<M>>, expected: List<M>) {
        val subscriber = stream.test()
        testScheduler.triggerActions()

        subscriber.assertValue(expected)
    }

    private fun shouldNeverComplete(streamProvider: MediaDao.() -> Flowable<out Any>) {
        // Given a realistic provider...
        val mediaDao = MediaDaoImpl(TestMediaProvider())

        // When subscribing and waiting for a long time...
        val subscriber = mediaDao.streamProvider().test()
        testScheduler.advanceTimeBy(3L, TimeUnit.DAYS)

        // it should never complete.
        subscriber.assertNotComplete()
    }

    private fun shouldUnregisterObserverOnStreamFailure(
        type: MediaType,
        streamProvider: MediaDao.() -> Flowable<out Any>
    ) {
        // Given an active subscription...
        val revokingPermissionProvider = TestMediaProvider()
        val mediaDao = MediaDaoImpl(revokingPermissionProvider)
        mediaDao.streamProvider().test()
        testScheduler.triggerActions()

        // When receiving an update that triggers an error...
        revokingPermissionProvider.hasStoragePermission = false
        revokingPermissionProvider.notifyChange(type)
        testScheduler.triggerActions()

        // Then unregister observer of the subscribed stream.
        assertThat(revokingPermissionProvider.registeredObservers)
            .comparingElementsUsing(THEIR_MEDIA_TYPE)
            .doesNotContain(type)
    }

    private fun shouldTerminateIfPermissionIsDenied(streamProvider: MediaDao.() -> Flowable<out Any>) {
        val deniedProvider = TestMediaProvider()
        deniedProvider.hasStoragePermission = false

        val mediaDao = MediaDaoImpl(deniedProvider)

        val subscriber = mediaDao.streamProvider().test()
        testScheduler.triggerActions()

        subscriber.assertError(PermissionDeniedException::class.java)
    }

    private fun shouldTerminateWhenPermissionIsRevoked(
        type: MediaType,
        streamProvider: MediaDao.() -> Flowable<out Any>
    ) {
        // Given an active subscription...
        val revokingPermissionProvider = TestMediaProvider()
        val mediaDao = MediaDaoImpl(revokingPermissionProvider)
        val subscriber = mediaDao.streamProvider().test()
        testScheduler.triggerActions()

        // When permission is revoked and an update notification is received...
        revokingPermissionProvider.hasStoragePermission = false
        revokingPermissionProvider.notifyChange(type)
        testScheduler.triggerActions()

        // Subscriber should be notified of an error.
        subscriber.assertError(PermissionDeniedException::class.java)
    }
}

private val THEIR_MEDIA_TYPE = Correspondence.transforming<Observer, MediaType>(
    { it!!.type },
    "has a type of"
)