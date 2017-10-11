package fr.nihilus.music.media.builtin

import android.content.Context
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import fr.nihilus.music.R
import io.reactivex.Single
import javax.inject.Inject

class AutomaticPlaylists
@Inject internal constructor(
        private val context: Context,
        private val playlists: Set<@JvmSuppressWildcards BuiltinItem>
): BuiltinItem {

    override fun asMediaItem(): MediaItem {
        val description = MediaDescriptionCompat.Builder()
                .setTitle(context.getString(R.string.title_automatic_playlists))
                .build()
        return MediaItem(description, MediaItem.FLAG_BROWSABLE)
    }

    override fun getChildren(): Single<List<MediaItem>> {
        return Single.fromCallable {
            playlists.map { it.asMediaItem() }
        }
    }
}