package fr.nihilus.music

import android.net.Uri
import android.support.test.filters.SmallTest
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import fr.nihilus.music.media.MediaItems
import fr.nihilus.music.media.source.MusicDao
import org.junit.Test

@SmallTest
class ExtensionTest {

    @Test
    fun mediaMetadata_asMediaDescription() {
        val metadata = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "42")
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Thunderstruck")
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "The Razors Edge")
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "ACDC")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 292_000L)
                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, 1L)
                .putLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER, 1L)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, "content://path/to/art")
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, "content://path/to/file")
                .putString(MusicDao.CUSTOM_META_TITLE_KEY, "Thunderstruck")
                .putLong(MusicDao.CUSTOM_META_ALBUM_ID, 12L)
                .putLong(MusicDao.CUSTOM_META_ARTIST_ID, 6L)
                .build()

        val desc = metadata.asMediaDescription(MediaDescriptionCompat.Builder(), "Music")

        assertMediaDescription(desc,
                mediaId = "Music|42",
                title = "Thunderstruck",
                subtitle = "ACDC",
                mediaUri = Uri.parse("content://path/to/file"),
                iconUri = Uri.parse("content://path/to/art"),
                extras = mapOf(
                        MediaItems.EXTRA_TITLE_KEY to "Thunderstruck",
                        MediaItems.EXTRA_DURATION to 292_000L,
                        MediaItems.EXTRA_TRACK_NUMBER to 1L,
                        MediaItems.EXTRA_DISC_NUMBER to 1L
                ))
    }
}