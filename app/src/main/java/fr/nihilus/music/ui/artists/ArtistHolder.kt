package fr.nihilus.music.ui.artists

import android.support.v4.app.Fragment
import android.support.v4.media.MediaBrowserCompat
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import fr.nihilus.music.R
import fr.nihilus.music.inflate
import fr.nihilus.music.media.MediaItems
import fr.nihilus.music.ui.MediaItemHolder

class ArtistHolder(fragment: Fragment, parent: ViewGroup)
    : MediaItemHolder(fragment, parent.inflate(R.layout.artist_grid_item)) {

    private val artistName = itemView.findViewById<TextView>(R.id.artistName)
    private val subtitle = itemView.findViewById<TextView>(R.id.subtitle)
    private val cover = itemView.findViewById<ImageView>(R.id.cover)

    override fun bind(item: MediaBrowserCompat.MediaItem) {
        val trackCount = item.description.extras!!.getInt(MediaItems.EXTRA_NUMBER_OF_TRACKS)
        artistName.text = item.description.title
        subtitle.text = itemView.resources.getQuantityString(R.plurals.number_of_tracks,
                trackCount, trackCount)

        // TODO Missing arguments
        Glide.with(fragment)
                .load(item.description.iconUri).asBitmap()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .centerCrop()
                .into(cover)
    }
}