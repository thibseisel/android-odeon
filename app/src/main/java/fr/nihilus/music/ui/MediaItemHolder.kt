package fr.nihilus.music.ui

import android.support.v4.media.MediaBrowserCompat
import android.support.v7.widget.RecyclerView
import android.view.View

/**
 * A ViewHolder specifically designed to display a media item.
 */
abstract class MediaItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    /**
     * Make this view holder reflect the state of a given media item.
     * @param item the media item this view holder must reflect
     */
    abstract fun bind(item: MediaBrowserCompat.MediaItem)
}