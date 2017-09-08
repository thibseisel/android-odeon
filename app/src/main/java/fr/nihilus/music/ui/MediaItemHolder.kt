package fr.nihilus.music.ui

import android.support.v4.app.Fragment
import android.support.v4.media.MediaBrowserCompat
import android.support.v7.widget.RecyclerView
import android.view.View

/**
 * A ViewHolder specifically designed to display a media item.
 * @constructor Creates a new holder associated with the given fragment
 * and displaying item in the specified view hierarchy.
 * @param fragment the fragment that contain the RecyclerView to which this holder is attached
 * @param itemView the view hierarchy this holder will use
 */
abstract class MediaItemHolder(
        protected val fragment: Fragment,
        itemView: View)
    : RecyclerView.ViewHolder(itemView) {

    /**
     * Make this view holder reflect the state of a given media item.
     * @param item the media item this view holder must reflect
     */
    abstract fun bind(item: MediaBrowserCompat.MediaItem)
}