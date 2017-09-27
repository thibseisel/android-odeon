package fr.nihilus.music

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.annotation.LayoutRes
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import fr.nihilus.music.media.MediaItems
import fr.nihilus.music.media.source.MusicDao
import fr.nihilus.music.utils.MediaID

/**
 * Convert this [MediaMetadataCompat] into its [MediaDescriptionCompat] equivalent.
 * @param categories the Media ID to use a prefix for this item's Media ID
 * @param builder an optional builder for reuse
 * @return a media description created from this track metadatas
 */
fun MediaMetadataCompat.asMediaDescription(
        builder: MediaDescriptionCompat.Builder,
        vararg categories: String
): MediaDescriptionCompat {
    val musicId = getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
    val extras = Bundle(4)
    extras.putString(MediaItems.EXTRA_TITLE_KEY, getString(MusicDao.CUSTOM_META_TITLE_KEY))
    extras.putLong(MediaItems.EXTRA_DURATION, getLong(MediaMetadataCompat.METADATA_KEY_DURATION))
    extras.putLong(MediaItems.EXTRA_TRACK_NUMBER, getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER))
    extras.putLong(MediaItems.EXTRA_DISC_NUMBER, getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER))
    val artUri = getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)

    val mediaId = MediaID.createMediaId(categories, musicId)
    builder.setMediaId(mediaId)
            .setTitle(getString(MediaMetadataCompat.METADATA_KEY_TITLE))
            .setSubtitle(getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
            .setMediaUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon()
                    .appendEncodedPath(musicId)
                    .build())
            .setExtras(extras)
    artUri?.let { builder.setIconUri(Uri.parse(it)) }

    return builder.build()
}

/**
 * Inflate the given layout as a child of this view group.
 *
 * @receiver the view parent in which inflate this layout
 * @param resource id for an XML resource to load
 * @param attach whether the inflated layout should be attached to this view group.
 * If false, the view group will be used to create the correct layout params.
 * @return the root view of the inflated hierarchy. If [attach] is `true`,
 * this will be this view group, otherwise the root of the inflated XML file.
 */
fun ViewGroup.inflate(@LayoutRes resource: Int, attach: Boolean = false): View =
        LayoutInflater.from(context).inflate(resource, this, attach)