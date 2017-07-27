package fr.nihilus.mymusic.media

import android.net.Uri.parse
import android.support.test.runner.AndroidJUnit4
import android.support.v4.media.MediaMetadataCompat
import fr.nihilus.mymusic.utils.MediaID
import io.reactivex.Observable
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

    private var testSubject: MusicRepository? = null
    private var mockDao: MediaDao? = null

    @Before
    fun setUp() {
        mockDao = mock(MediaDao::class.java)
        testSubject = MusicRepository(mockDao!!)
    }

    @Test
    fun mediaMetadataToMediaItem() {
        val values = METADATA[0]
        val metadata = sampleToMetadata(values)

        val mediaItem = metadata.asMediaItem(MediaID.ID_MUSIC)
        assertThat(mediaItem.mediaId, equalTo("${MediaID.ID_MUSIC}/${values[0]}"))
        assertThat(mediaItem.description.title, equalTo(values[1] as CharSequence))
        assertThat(mediaItem.description.subtitle, equalTo(values[3] as CharSequence))
        assertThat(mediaItem.description.mediaUri, equalTo(parse(values[8] as String)))

        val extras = mediaItem.description.extras!!
        assertThat(extras.getString(MediaItems.EXTRA_TITLE_KEY), equalTo(values[9]))
        assertThat(extras.getLong(MediaItems.EXTRA_DURATION), equalTo(values[4]))
    }

    @Test
    fun getMediaItems() {
        val observableTracks = Observable.just(listOf(
                sampleToMetadata(METADATA[0]),
                sampleToMetadata(METADATA[1])
        ))
        `when`(mockDao!!.getAllTracks()).thenReturn(observableTracks)

        TODO("Add verification logic")
    }

    @After
    fun tearDown() {
        testSubject?.clear()
        mockDao = null
        testSubject = null
    }

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
}

// Test materials
private val METADATA = arrayOf(
        arrayOf("1", "Title", "Album", "Artist", 12345L, 4L, 1L,
                "content://path/to/album/art", "content//uri/to/track", "TitleKey", 2L, 5L),
        arrayOf("2", "Amerika", "Reise Reise", "Rammstein", 234567L, 6L, 1L,
                "content://path/to/amerika", "content://uri/to/amerika", "Amerika", 3L, 8L)
)