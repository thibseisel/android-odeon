package fr.nihilus.music.media.builtin

import android.content.Context
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import fr.nihilus.music.R
import fr.nihilus.music.asMediaDescription
import fr.nihilus.music.media.cache.MusicCache
import fr.nihilus.music.media.source.MusicDao
import fr.nihilus.music.utils.MediaID
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject

/**
 *
 */
internal class AllTracks
@Inject constructor(
        private val context: Context,
        private val musicDao: MusicDao,
        private val cache: MusicCache
) : BuiltinItem {

    override fun asMediaItem(): MediaItem {
        val description = MediaDescriptionCompat.Builder()
                .setMediaId(MediaID.ID_MUSIC)
                .setTitle(context.getString(R.string.all_music))
                .build()
        return MediaItem(description, MediaItem.FLAG_BROWSABLE or MediaItem.FLAG_PLAYABLE)
    }

    override fun getChildren(): Single<List<MediaItem>> {
        val builder = MediaDescriptionCompat.Builder()
        return musicDao.getAllTracks()
                .flatMap { Observable.fromIterable(it) }
                .doOnNext { metadata ->
                    val musicId = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                    cache.putMetadata(musicId, metadata)
                }
                .map { metadata ->
                    val description = metadata.asMediaDescription(builder, MediaID.ID_MUSIC)
                    MediaItem(description, MediaItem.FLAG_PLAYABLE)
                }.toList()
    }
}