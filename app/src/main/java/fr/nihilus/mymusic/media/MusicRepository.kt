package fr.nihilus.mymusic.media

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import fr.nihilus.mymusic.database.PlaylistDao
import fr.nihilus.mymusic.utils.MediaID
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository
@Inject internal constructor(val mediaDao: MediaDao, val playlistDao: PlaylistDao) {

    // TODO Caching of MediaMetadata items

    fun getMediaItems(parentMediaId: String): Observable<List<MediaItem>> {
        return when (parentMediaId) {
            MediaID.ID_MUSIC -> mediaDao.getAllTracks().map(this::asMediaItem)
            else -> Observable.just(null)
        }
    }

    private fun asMediaItem(metadataList: List<MediaMetadataCompat>): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()
        val items = ArrayList<MediaItem>(metadataList.size)

        for (meta in metadataList) {
            val musicId = meta.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
            val extras = Bundle(2)
            extras.putString(MediaItems.EXTRA_TITLE_KEY, meta.getString(MediaDao.CUSTOM_META_TITLE_KEY))
            extras.putLong(MediaItems.EXTRA_DURATION, meta.getLong(MediaMetadataCompat.METADATA_KEY_DURATION))
            val artUri = meta.getString(MediaMetadataCompat.METADATA_KEY_ART_URI)

            builder.setMediaId(MediaID.createMediaID(musicId, MediaID.ID_MUSIC))
                    .setTitle(meta.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
                    .setSubtitle(meta.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
                    .setMediaUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon()
                            .appendEncodedPath(musicId)
                            .build())
                    .setExtras(extras)
            artUri?.let { builder.setIconUri(Uri.parse(it)) }

            items.add(MediaItem(builder.build(), MediaItem.FLAG_PLAYABLE))
        }

        return items
    }
}
