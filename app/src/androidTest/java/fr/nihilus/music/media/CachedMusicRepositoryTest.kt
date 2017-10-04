package fr.nihilus.music.media

import android.net.Uri
import android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import fr.nihilus.music.asMediaDescription
import fr.nihilus.music.database.PlaylistDao
import fr.nihilus.music.media.cache.MusicCache
import fr.nihilus.music.media.repo.CachedMusicRepository
import fr.nihilus.music.media.source.MediaStoreMusicDao
import fr.nihilus.music.media.source.MusicDao
import fr.nihilus.music.utils.MediaID
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations

@SmallTest
@RunWith(AndroidJUnit4::class)
class CachedMusicRepositoryTest {

    /** A Subject allowing the tester to emulate the emission of new metadata from [MediaStoreMusicDao]. */
    lateinit var metadataSubject: Subject<List<MediaMetadataCompat>>
    private lateinit var subject: CachedMusicRepository

    @Mock lateinit var cache: MusicCache
    @Mock lateinit var dao: MusicDao
    @Mock lateinit var playlistDao: PlaylistDao

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        metadataSubject = PublishSubject.create()
        subject = CachedMusicRepository(dao, cache, playlistDao, emptyMap())
    }

    /**
     * Check proper transformation of a MediaMetadataCompat into a MediaItem via
     * the [CachedMusicRepository.asMediaDescription] extension function.
     */
    @Test
    fun mediaMetadataToMediaDescription() {
        val values = METADATA[0]
        val metadata = sampleToMetadata(values)

        val description = metadata.asMediaDescription(MediaDescriptionCompat.Builder(), MediaID.ID_MUSIC)
        assertThat(description.mediaId, equalTo("${MediaID.ID_MUSIC}|${values[0]}"))
        assertThat(description.title, equalTo(values[1] as CharSequence))
        assertThat(description.subtitle, equalTo(values[3] as CharSequence))
        assertThat(description.mediaUri, equalTo(Uri.parse(values[8] as String)))

        val extras = description.extras!!
        assertThat(extras.getString(MediaItems.EXTRA_TITLE_KEY), equalTo(values[9]))
        assertThat(extras.getLong(MediaItems.EXTRA_DURATION), equalTo(values[4]))
    }

    /**
     * Assert that [CachedMusicRepository.getMediaItems] emits a list of media items
     * corresponding to tracks fetched from [MediaStoreMusicDao] when the media ID is [MediaID.ID_MUSIC].
     */
    @Test
    fun mediaItems_allTracks() {
        TODO("API has changed: need to rewrite test.")
        // Subscribe to this Single with a TestObserver
        /*val testObserver = subject.getMediaChildren(MediaID.ID_MUSIC).test()

        // Push one event
        metadataSubject.onNext(listOf(
                sampleToMetadata(METADATA[0]),
                sampleToMetadata(METADATA[1])
        ))

        // Assert that the observer completed with the pushed values as MediaItems
        testObserver.assertValue { items ->
            items.size == 2
                    && items[0].mediaId == "${MediaID.ID_MUSIC}|1"
                    && items[1].mediaId == "${MediaID.ID_MUSIC}|2"
        }.assertComplete()*/
    }

    @Test
    fun mediaItems_allAlbums() {
        TODO("API has changed: need to rewrite test.")
        // val testObserver = subject.getMediaChildren(MediaID.ID_ALBUMS).test()
    }

    @Test
    fun mediaItems_unsupportedMediaIdThrows() {
        /*subject.getMediaChildren("Unknown").test()
                .assertError(UnsupportedOperationException::class.java)*/
    }

    @Test
    fun metadata_getFromDao() {
        val requestedMetadata = sampleToMetadata(METADATA[0])

        val testObserver = subject.getMetadata("1").test()
        metadataSubject.onNext(listOf(requestedMetadata, sampleToMetadata(METADATA[1])))

        testObserver.assertValue(requestedMetadata)
                .assertComplete()
    }

    @Test
    fun metadata_getFromCache() {
        val requestedMetadata = sampleToMetadata(METADATA[0])

        // Pre-fetch metadata in repository
        /*subject.getMediaChildren(MediaID.ID_MUSIC).subscribe()

        val testObserver = subject.getMetadata(1L).test()
        metadataSubject.onNext(listOf(requestedMetadata, sampleToMetadata(METADATA[1])))

        testObserver.assertValue(requestedMetadata)
                .assertComplete()*/
    }

    @Test
    fun metadata_errorIfNotFound() {
        subject.getMetadata("3").test()
                .assertError(RuntimeException::class.java)
                .assertTerminated()
    }

    /**
     * Assert that events from [CachedMusicRepository.mediaChanges] are shared
     * between multiple observers.
     */
    @Test
    fun mediaChanges_sharedSubscription() {
        val firstMeta = sampleToMetadata(METADATA[0])
        val secondMeta = sampleToMetadata(METADATA[1])

        val firstObserver = subject.mediaChanges.test()
        metadataSubject.onNext(listOf(firstMeta))
        firstObserver.assertValueCount(1)

        val secondObserver = subject.mediaChanges.test()
        metadataSubject.onNext(listOf(secondMeta))
        firstObserver.assertValueCount(2)
        secondObserver.assertValueCount(1)
        assertThat(firstObserver.values()[1] === secondObserver.values()[0], `is`(true))

        firstObserver.dispose()
        metadataSubject.onNext(listOf(firstMeta))
        firstObserver.assertValueCount(2)
        secondObserver.assertValueCount(2)

        secondObserver.dispose()
    }

    /**
     * Assert that a change in track metadata in the repository
     * triggers a notification with [MediaID.ID_MUSIC].
     */
    @Test
    fun mediaChanges_notifyChangeInTracks() {
        val testObserver = subject.mediaChanges.test()
        metadataSubject.onNext(emptyList())
        testObserver.assertValue(MediaID.ID_MUSIC)

        testObserver.dispose()
    }

    @After
    fun tearDown() {
        subject.clear()
    }

    private fun provideMockDao(): MediaStoreMusicDao {
        val mock = mock(MediaStoreMusicDao::class.java)
        `when`(mock.getAllTracks()).thenReturn(metadataSubject)
        return mock
    }

}

// Test materials
private val METADATA = arrayOf(
        arrayOf("1", "Title", "Album", "Artist", 12345L, 4L, 1L,
                "content://path/to/album/art", mediaUriOf(1L), "TitleKey", 2L, 5L),
        arrayOf("2", "Amerika", "Reise Reise", "Rammstein", 234567L, 6L, 1L,
                "content://path/to/amerika", mediaUriOf(2L), "Amerika", 3L, 8L)
)

private fun mediaUriOf(musicId: Long) = "$EXTERNAL_CONTENT_URI/$musicId"

/**
 * Translate sample data contained in the [METADATA] array into a [MediaMetadataCompat].
 */
private fun sampleToMetadata(values: Array<Any>): MediaMetadataCompat {
    return MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, values[0] as String)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, values[1] as String)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, values[2] as String)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, values[3] as String)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, values[4] as Long)
            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, values[5] as Long)
            .putLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER, values[6] as Long)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, values[7] as String)
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, values[8] as String)
            .putString(MusicDao.CUSTOM_META_TITLE_KEY, values[9] as String)
            .putLong(MusicDao.CUSTOM_META_ALBUM_ID, values[10] as Long)
            .putLong(MusicDao.CUSTOM_META_ARTIST_ID, values[11] as Long)
            .build()
}
