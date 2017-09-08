package fr.nihilus.music.ui

import android.support.v4.app.Fragment
import android.support.v4.media.MediaBrowserCompat
import android.view.ViewGroup
import fr.nihilus.music.ui.albums.AlbumHolder
import fr.nihilus.music.utils.MediaID

class ViewHolderFactory(private val fragment: Fragment) {

    fun create(parent: ViewGroup, viewType: Int): MediaItemHolder = when (viewType) {
        TYPE_ALBUM -> AlbumHolder(fragment, parent)
        TYPE_ARTIST -> TODO()
        TYPE_ALBUM_TRACK -> TODO()
        TYPE_ARTIST_TRACK -> TODO()
        else -> throw UnsupportedOperationException()
    }

    fun viewTypeFor(item: MediaBrowserCompat.MediaItem): Int {
        val mediaId = item.mediaId
                ?: throw IllegalStateException("Media item should have a mediaId")
        return when {
            mediaId.startsWith(MediaID.ID_MUSIC) -> TYPE_MUSIC
            mediaId.startsWith(MediaID.ID_ALBUMS) ->
                if (MediaID.isBrowseable(mediaId)) TYPE_ALBUM
                else TYPE_ALBUM_TRACK
            mediaId.startsWith(MediaID.ID_ARTISTS) ->
                if (MediaID.isBrowseable(mediaId)) TYPE_ARTIST
                else TYPE_ARTIST_TRACK
            else -> TYPE_UNKNOWN
        }
    }

    companion object {
        const val TYPE_UNKNOWN = -1
        const val TYPE_MUSIC = 0
        const val TYPE_ALBUM = 1
        const val TYPE_ALBUM_TRACK = 2
        const val TYPE_ARTIST = 3
        const val TYPE_ARTIST_TRACK = 4
    }
}