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
import fr.nihilus.music.media.RxSchedulers
import fr.nihilus.music.media.permissions.PermissionDeniedException
import fr.nihilus.music.media.provider.MediaProvider.MediaType
import fr.nihilus.music.media.provider.MediaProvider.Observer
import io.reactivex.Flowable
import io.reactivex.schedulers.TestScheduler
import org.junit.Test
import java.util.concurrent.TimeUnit

class RxMediaDaoTest {

    private val testScheduler = TestScheduler()
    private val schedulers = RxSchedulers(testScheduler)

    @Test
    fun givenDeniedPermission_whenSubscribingTracks_thenTerminateWithPermissionDeniedException() {
        shouldTerminateIfPermissionIsDenied(RxMediaDao::tracks)
    }

    @Test
    fun givenDeniedPermission_whenSubscribingAlbums_thenTerminateWithPermissionDeniedException() {
        shouldTerminateIfPermissionIsDenied(RxMediaDao::albums)
    }

    @Test
    fun givenDeniedPermission_whenSubscribingArtists_thenTerminateWithPermissionDeniedException() {
        shouldTerminateIfPermissionIsDenied(RxMediaDao::artists)
    }

    @Test
    fun givenTrackSubscription_whenReceivingUpdateAndPermissionIsRevoked_thenTerminateWithPermissionDeniedException() {
        shouldTerminateWhenPermissionIsRevoked(MediaType.TRACKS, RxMediaDao::tracks)
    }

    @Test
    fun givenAlbumSubscription_whenReceivingUpdateAndPermissionIsRevoked_thenTerminateWithPermissionDeniedException() {
        shouldTerminateWhenPermissionIsRevoked(MediaType.ALBUMS, RxMediaDao::albums)
    }

    @Test
    fun givenArtistSubscription_whenReceivingUpdateAndPermissionIsRevoked_thenTerminateWithPermissionDeniedException() {
        shouldTerminateWhenPermissionIsRevoked(MediaType.ARTISTS, RxMediaDao::artists)
    }

    @Test
    fun givenTrackSubscription_whenTerminatingWithError_thenUnregisterObserver() {
        shouldUnregisterObserverOnStreamFailure(MediaType.TRACKS, RxMediaDao::tracks)
    }

    @Test
    fun givenAlbumSubscription_whenTerminatingWithError_thenUnregisterObserver() {
        shouldUnregisterObserverOnStreamFailure(MediaType.ALBUMS, RxMediaDao::albums)
    }

    @Test
    fun givenArtistSubscription_whenTerminatingWithError_thenUnregisterObserver() {
        shouldUnregisterObserverOnStreamFailure(MediaType.ARTISTS, RxMediaDao::artists)
    }

    @Test
    fun whenSubscribingTracks_thenStreamShouldNeverComplete() {
        shouldNeverComplete(RxMediaDao::tracks)
    }

    @Test
    fun whenSubscribingAlbums_thenStreamShouldNeverComplete() {
        shouldNeverComplete(RxMediaDao::albums)
    }

    @Test
    fun whenSubscribingArtists_thenStreamShouldNeverComplete() {
        shouldNeverComplete(RxMediaDao::artists)
    }

    @Test
    fun whenSubscribingTracks_thenLoadCurrentTrackList() {
        val mediaDao = RxMediaDaoImpl(TestMediaProvider(tracks = SAMPLE_TRACKS), schedulers)
        shouldLoadMediaOnSubscription(mediaDao.tracks, SAMPLE_TRACKS)
    }

    @Test
    fun whenSubscribingAlbums_thenLoadCurrentAlbumList() {
        val mediaDao = RxMediaDaoImpl(TestMediaProvider(albums = SAMPLE_ALBUMS), schedulers)
        shouldLoadMediaOnSubscription(mediaDao.albums, SAMPLE_ALBUMS)
    }

    @Test
    fun whenSubscribingArtists_thenLoadCurrentArtistList() {
        val mediaDao = RxMediaDaoImpl(TestMediaProvider(artists = SAMPLE_ARTISTS), schedulers)
        shouldLoadMediaOnSubscription(mediaDao.artists, SAMPLE_ARTISTS)
    }

    @Test
    fun givenTrackSubscription_whenNotifiedForChange_thenReloadTrackList() {
        val provider = TestMediaProvider()
        val mediaDao = RxMediaDaoImpl(provider, schedulers)
        shouldReloadMediaWhenNotified(provider, MediaType.TRACKS, mediaDao.tracks, SAMPLE_TRACKS)
    }

    @Test
    fun givenAlbumSubscription_whenNotifiedForChange_thenReloadAlbumList() {
        val provider = TestMediaProvider()
        val mediaDao = RxMediaDaoImpl(provider, schedulers)
        shouldReloadMediaWhenNotified(provider, MediaType.ALBUMS, mediaDao.albums, SAMPLE_ALBUMS)
    }

    @Test
    fun givenArtistSubscription_whenNotifiedForChange_thenReloadArtistList() {
        val provider = TestMediaProvider()
        val mediaDao = RxMediaDaoImpl(provider, schedulers)
        shouldReloadMediaWhenNotified(provider, MediaType.ARTISTS, mediaDao.artists, SAMPLE_ARTISTS)
    }

    @Test
    fun whenSubscribingTracks_thenRegisterAnObserver() {
        shouldRegisterAnObserverWhenSubscribed(MediaType.TRACKS, RxMediaDao::tracks)
    }

    @Test
    fun whenSubscribingAlbums_thenRegisterAnObserver() {
        shouldRegisterAnObserverWhenSubscribed(MediaType.ALBUMS, RxMediaDao::albums)
    }

    @Test
    fun whenSubscribingArtists_thenRegisterAnObserver() {
        shouldRegisterAnObserverWhenSubscribed(MediaType.ARTISTS, RxMediaDao::artists)
    }

    @Test
    fun givenAnActiveTrackSubscription_whenDisposingIt_thenUnregisterObserver() {
        shouldUnregisterObserverOnDisposal(MediaType.TRACKS, RxMediaDao::tracks)
    }

    @Test
    fun givenAnActiveAlbumSubscription_whenDisposingIt_thenUnregisterObserver() {
        shouldUnregisterObserverOnDisposal(MediaType.ALBUMS, RxMediaDao::albums)
    }

    @Test
    fun givenAnActiveArtistSubscription_whenDisposingIt_thenUnregisterObserver() {
        shouldUnregisterObserverOnDisposal(MediaType.ARTISTS, RxMediaDao::artists)
    }

    @Test
    fun givenDeniedPermission_whenDeletingTracks_thenFailWithPermissionDeniedException() {
        val provider = TestMediaProvider()
        provider.hasStoragePermission = false
        val mediaDao = RxMediaDaoImpl(provider, schedulers)

        val observer = mediaDao.deleteTracks(longArrayOf(161L)).test()
        testScheduler.triggerActions()

        observer.assertError(PermissionDeniedException::class.java)
    }

    private fun shouldUnregisterObserverOnDisposal(
        observerType: MediaType,
        streamProvider: RxMediaDao.() -> Flowable<out Any>
    ) {
        val provider = TestMediaProvider()
        val mediaDao = RxMediaDaoImpl(provider, schedulers)

        val subscriber = mediaDao.streamProvider().test()
        testScheduler.triggerActions()
        subscriber.dispose()

        assertThat(provider.registeredObservers).comparingElementsUsing(THEIR_MEDIA_TYPE)
            .doesNotContain(observerType)
    }

    private fun shouldRegisterAnObserverWhenSubscribed(
        observerType: MediaType,
        streamProvider: RxMediaDao.() -> Flowable<out Any>
    ) {
        val provider = TestMediaProvider()
        val mediaDao = RxMediaDaoImpl(provider, schedulers)

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
        testScheduler.triggerActions()

        provider.notifyChange(type)
        testScheduler.triggerActions()

        subscriber.assertValueAt(1, expected)
    }

    private fun <M> shouldLoadMediaOnSubscription(stream: Flowable<List<M>>, expected: List<M>) {
        val subscriber = stream.test()
        testScheduler.triggerActions()

        subscriber.assertValue(expected)
    }

    private fun shouldNeverComplete(streamProvider: RxMediaDao.() -> Flowable<out Any>) {
        // Given a realistic provider...
        val mediaDao = RxMediaDaoImpl(TestMediaProvider(), schedulers)

        // When subscribing and waiting for a long time...
        val subscriber = mediaDao.streamProvider().test()
        testScheduler.advanceTimeBy(3L, TimeUnit.DAYS)

        // it should never complete.
        subscriber.assertNotComplete()
    }

    private fun shouldUnregisterObserverOnStreamFailure(
        type: MediaType,
        streamProvider: RxMediaDao.() -> Flowable<out Any>
    ) {
        // Given an active subscription...
        val revokingPermissionProvider = TestMediaProvider()
        val mediaDao = RxMediaDaoImpl(revokingPermissionProvider, schedulers)
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

    private fun shouldTerminateIfPermissionIsDenied(streamProvider: RxMediaDao.() -> Flowable<out Any>) {
        val deniedProvider = TestMediaProvider()
        deniedProvider.hasStoragePermission = false

        val mediaDao = RxMediaDaoImpl(deniedProvider, schedulers)

        val subscriber = mediaDao.streamProvider().test()
        testScheduler.triggerActions()

        subscriber.assertError(PermissionDeniedException::class.java)
    }

    private fun shouldTerminateWhenPermissionIsRevoked(
        type: MediaType,
        streamProvider: RxMediaDao.() -> Flowable<out Any>
    ) {
        // Given an active subscription...
        val revokingPermissionProvider = TestMediaProvider()
        val mediaDao = RxMediaDaoImpl(revokingPermissionProvider, schedulers)
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