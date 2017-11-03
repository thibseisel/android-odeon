package fr.nihilus.music.media.builtin

import android.content.Context
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import fr.nihilus.music.R
import fr.nihilus.music.utils.MediaID
import io.reactivex.Single
import java.util.*
import javax.inject.Inject

/**
 * A super-set of the [AllTracks] playlist that plays all tracks in random order.
 */
internal class AllTracksRandom
@Inject constructor(
        private val context: Context,
        private val allTracks: AllTracks
) : BuiltinItem {

    override fun asMediaItem(): MediaItem {
        val description = MediaDescriptionCompat.Builder()
                .setMediaId(MediaID.ID_RANDOM)
                .setTitle(context.getString(R.string.play_all_shuffled))
                .build()
        return MediaItem(description, MediaItem.FLAG_PLAYABLE)
    }

    override fun getChildren(parentMediaId: String): Single<List<MediaItem>> {
        return allTracks.getChildren(parentMediaId).map { list ->
            list.toMutableList().also { Collections.shuffle(it) }
        }
    }


}