package fr.nihilus.music.media

import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat

private val descrBuilder = MediaDescriptionCompat.Builder()

/**
 * Create a [MediaDescriptionCompat] from the passed parameters.
 */
internal fun mediaDescriptionOf(
        mediaId: String? = null,
        title: CharSequence? = null,
        subtitle: CharSequence? = null,
        description: CharSequence? = null,
        iconUri: Uri? = null,
        mediaUri: Uri? = null,
        extras: Bundle? = null): MediaDescriptionCompat {

    return descrBuilder.setMediaId(mediaId)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setIconUri(iconUri)
            .setMediaUri(mediaUri)
            .setExtras(extras)
            .build()
}

/**
 * Create an [Uri] that points to the album artwork of a given album.
 * This can be used as a parameter for methods asking for iconUri such as [mediaDescriptionOf].
 * @param albumId unique identifier of the album represented by the artwork
 */
internal fun artUriOf(albumId: Long): Uri {
    return Uri.parse("content://media/external/audio/albumart/$albumId")
}

/**
 * Assert that the given [MediaMetadataCompat] has the expected mediaId, title, album, artist,
 * duration, disc number, track number and album art Uri.
 */
internal fun assertMetadata(meta: MediaMetadataCompat, mediaId: String, title: String,
                           album: String, artist: String, duration: Long, discNo: Long,
                           trackNo: Long, artUri: String) {
    assertThat(meta.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID), equalTo(mediaId))
    assertThat(meta.getString(MediaMetadataCompat.METADATA_KEY_TITLE), equalTo(title))
    assertThat(meta.getString(MediaMetadataCompat.METADATA_KEY_ALBUM), equalTo(album))
    assertThat(meta.getString(MediaMetadataCompat.METADATA_KEY_ARTIST), equalTo(artist))
    assertThat(meta.getLong(MediaMetadataCompat.METADATA_KEY_DURATION), equalTo(duration))
    assertThat(meta.getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER), equalTo(discNo))
    assertThat(meta.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER), equalTo(trackNo))
    assertThat(meta.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI), equalTo(artUri))
}

