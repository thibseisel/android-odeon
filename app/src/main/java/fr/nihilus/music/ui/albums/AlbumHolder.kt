package fr.nihilus.music.ui.albums

import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.ImageViewTarget
import fr.nihilus.music.R
import fr.nihilus.music.palette.BottomPaletteTranscoder
import fr.nihilus.music.palette.PaletteBitmap
import fr.nihilus.music.ui.MediaItemHolder

class AlbumHolder(parent: ViewGroup) : MediaItemHolder(LayoutInflater.from(parent.context)
        .inflate(R.layout.album_grid_item, parent, false)) {

    private val albumArt = itemView.findViewById<ImageView>(R.id.cover)
    private val band = itemView.findViewById<ViewGroup>(R.id.band)
    private val title = itemView.findViewById<TextView>(R.id.title)
    private val artist = itemView.findViewById<TextView>(R.id.artist)

    override fun bind(item: MediaBrowserCompat.MediaItem) {
        title.text = item.description.title
        artist.text = item.description.subtitle

        // TODO Use Fragment instead of context to bind images to the correct lifecycle
        // Might also inject a request builder loaded with Fragment's context
        Glide.with(itemView.context)
                .load(item.description.iconUri).asBitmap()
                .transcode(BottomPaletteTranscoder(itemView.context), PaletteBitmap::class.java)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .error(R.drawable.dummy_album_art)
                .into(object : ImageViewTarget<PaletteBitmap>(albumArt) {
                    override fun setResource(resource: PaletteBitmap) {
                        super.view.setImageBitmap(resource.bitmap)
                        resource.palette.dominantSwatch?.let {
                            band.setBackgroundColor(it.rgb)
                            title.setTextColor(it.bodyTextColor)
                            artist.setTextColor(it.bodyTextColor)
                        }
                    }
                })
    }

}