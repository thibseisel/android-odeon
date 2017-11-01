package fr.nihilus.music.media.builtin

import android.content.Context
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import fr.nihilus.music.R
import fr.nihilus.music.asMediaDescription
import fr.nihilus.music.media.source.MusicDao
import fr.nihilus.music.utils.MediaID
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject

internal class AlbumItems
@Inject constructor(
        private val context: Context,
        private val musicDao: MusicDao
): BuiltinItem {

    override fun asMediaItem(): MediaItem {
        val description = MediaDescriptionCompat.Builder()
                .setMediaId(MediaID.ID_ALBUMS)
                .setTitle(context.getString(R.string.action_albums))
                .build()
        return MediaItem(description, MediaItem.FLAG_BROWSABLE)
    }

    override fun getChildren(parentMediaId: String): Single<List<MediaItem>> {
        val hierarchy = MediaID.getHierarchy(parentMediaId)
        return if (hierarchy.size > 1) {
            val albumId = hierarchy[1]
            fetchAlbumTracks(albumId)
        } else {
            fetchAllAlbums()
        }
    }

    private fun fetchAllAlbums(): Single<List<MediaItem>> {
        return musicDao.getAlbums().flatMap { Observable.fromIterable(it) }
                .map { MediaItem(it, MediaItem.FLAG_BROWSABLE or MediaItem.FLAG_PLAYABLE) }
                .toList()
    }

    private fun fetchAlbumTracks(albumId: String): Single<List<MediaItem>> {
        val builder = MediaDescriptionCompat.Builder()
        return musicDao.getAlbumTracks(albumId)
                .flatMap { Observable.fromIterable(it) }
                .map { it.asMediaDescription(builder, MediaID.ID_ALBUMS, albumId) }
                .map { MediaItem(it, MediaItem.FLAG_PLAYABLE) }
                .toList()
    }
}