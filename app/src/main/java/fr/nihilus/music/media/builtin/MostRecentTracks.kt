package fr.nihilus.music.media.builtin

import android.content.Context
import android.provider.MediaStore.Audio.Media
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import fr.nihilus.music.R
import fr.nihilus.music.asMediaDescription
import fr.nihilus.music.media.source.MusicDao
import fr.nihilus.music.utils.MediaID
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject

/**
 * A built-in item that groups tracks that have been added to the music library recently.
 */
internal class MostRecentTracks
@Inject constructor(
        context: Context,
        private val dao: MusicDao
) : BuiltinItem {

    private val resources = context.resources

    override fun asMediaItem(): MediaItem {
        val builder = MediaDescriptionCompat.Builder()
        val description = builder
                .setMediaId(MediaID.ID_MOST_RECENT)
                .setTitle(resources.getText(R.string.last_added))
                .build()
        return MediaItem(description, MediaItem.FLAG_BROWSABLE)
    }

    override fun getChildren(parentMediaId: String): Single<List<MediaItem>> {
        val builder = MediaDescriptionCompat.Builder()
        return dao.getTracks(null, null, "${Media.DATE_ADDED} DESC").take(1)
                .flatMap { Observable.fromIterable(it) }
                .map {
                    val description = it.asMediaDescription(builder, MediaID.ID_MOST_RECENT)
                    MediaItem(description, MediaItem.FLAG_PLAYABLE)
                }.take(50)
                .toList()
    }
}