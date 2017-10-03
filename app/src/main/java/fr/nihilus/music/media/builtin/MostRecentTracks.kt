package fr.nihilus.music.media.builtin

import android.content.Context
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import fr.nihilus.music.R
import fr.nihilus.music.asMediaDescription
import fr.nihilus.music.media.source.MusicDao
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject

/**
 * A built-in item that groups tracks that have been added to the music library recently.
 */
class MostRecentTracks
@Inject internal constructor(
        context: Context,
        private val dao: MusicDao
) : BuiltinItem {

    private val resources = context.resources

    override fun asMediaItem(): MediaItem {
        val builder = MediaDescriptionCompat.Builder()
        val description = builder.setTitle(resources.getText(R.string.last_added)).build()
        return MediaItem(description, MediaItem.FLAG_BROWSABLE)
    }

    override fun getChildren(): Single<List<MediaItem>> {
        val builder = MediaDescriptionCompat.Builder()
        return dao.getTracks(null, null, MediaStore.Audio.Media.DATE_ADDED).take(1)
                .flatMap { Observable.fromIterable(it) }
                .map {
                    val description = it.asMediaDescription(builder)
                    MediaItem(description, MediaItem.FLAG_PLAYABLE)
                }.take(50)
                .toList()
    }
}