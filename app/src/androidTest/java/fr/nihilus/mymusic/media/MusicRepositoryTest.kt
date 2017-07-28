package fr.nihilus.mymusic.media

import android.net.Uri.parse
import android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
import android.support.test.runner.AndroidJUnit4
import android.support.v4.media.MediaMetadataCompat
import fr.nihilus.mymusic.utils.MediaID
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.hamcrest.Matchers.equalTo
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class MusicRepositoryTest {

    /** A Subject allowing the tester to emulate the emission of new metadata from [MediaDao]. */
    lateinit var metadataSubject: Subject<List<MediaMetadataCompat>>
    lateinit var testSubject: MusicRepository

    @Before
    fun setUp() {
        metadataSubject = PublishSubject.create()
        val mediaDao = provideMockDao()
        testSubject = MusicRepository(mediaDao)
    }

    /**
     * Check proper transformation of a MediaMetadataCompat into a MediaItem via
     * the [MusicRepository.asMediaItem] extension function.
     */
    @Test
    fun mediaMetadataToMediaItem() {
        val values = METADATA[0]
        val metadata = sampleToMetadata(values)

        val mediaItem = metadata.asMediaItem(MediaID.ID_MUSIC)
        assertThat(mediaItem.mediaId, equalTo("${MediaID.ID_MUSIC}|${values[0]}"))
        assertThat(mediaItem.description.title, equalTo(values[1] as CharSequence))
        assertThat(mediaItem.description.subtitle, equalTo(values[3] as CharSequence))
        assertThat(mediaItem.description.mediaUri, equalTo(parse(values[8] as String)))

        val extras = mediaItem.description.extras!!
        assertThat(extras.getString(MediaItems.EXTRA_TITLE_KEY), equalTo(values[9]))
        assertThat(extras.getLong(MediaItems.EXTRA_DURATION), equalTo(values[4]))
    }

    /**
     * Assert that [MusicRepository.getMediaItems] emits a list of media items
     * corresponding to tracks fetched from [MediaDao] when the media ID is [MediaID.ID_MUSIC].
     */
    @Test
    fun mediaItems_allTracks() {
        // Subscribe to this Single with a TestObserver
        val testObserver = testSubject.getMediaItems(MediaID.ID_MUSIC).test()

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
        }.assertComplete()
    }

    /**
     * Assert that the Single returned by [MusicRepository.getMediaItems]
     * emits an error notification when the passed parent media ID is unsupported.
     */
    @Test
    fun mediaItems_unsupportedMediaIdThrows() {
        testSubject.getMediaItems("Unknown").test()
                .assertError(UnsupportedOperationException::class.java)
    }

    /**
     * Assert that events from [MusicRepository.mediaChanges] are shared
     * between multiple observers.
     */
    @Test
    fun mediaChanges_sharedSubscription() {
        val firstObserver = testSubject.mediaChanges.test()
        metadataSubject.onNext(emptyList())
        firstObserver.assertValueCount(1)

        val secondObserver = testSubject.mediaChanges.test()
        metadataSubject.onNext(emptyList())
        firstObserver.assertValueCount(2)
        secondObserver.assertValueCount(1)

        firstObserver.dispose()
        metadataSubject.onNext(emptyList())
        firstObserver.assertValueCount(2)
        secondObserver.assertValueCount(2)

        firstObserver.dispose()
        secondObserver.dispose()
    }

    /**
     * Assert that a change in track metadata in the repository
     * triggers a notification with [MediaID.ID_MUSIC].
     */
    @Test
    fun mediaChanges_notifyChangeInTracks() {
        val testObserver = testSubject.mediaChanges.test()
        metadataSubject.onNext(emptyList())
        testObserver.assertValue(MediaID.ID_MUSIC)

        testObserver.dispose()
    }

    @After
    fun tearDown() {
        testSubject.clear()
    }

    private fun provideMockDao(): MediaDao {
        return mock(MediaDao::class.java).also {
            `when`(it.getAllTracks()).thenReturn(metadataSubject)
        }
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
            .putString(MediaDao.CUSTOM_META_TITLE_KEY, values[9] as String)
            .putLong(MediaDao.CUSTOM_META_ALBUM_ID, values[10] as Long)
            .putLong(MediaDao.CUSTOM_META_ARTIST_ID, values[11] as Long)
            .build()
}
