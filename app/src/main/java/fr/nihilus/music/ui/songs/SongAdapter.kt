package fr.nihilus.music.ui.songs

import android.graphics.Bitmap
import android.support.v4.app.Fragment
import android.support.v4.media.MediaBrowserCompat
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.SectionIndexer
import android.widget.TextView
import fr.nihilus.music.R
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.glide.GlideRequest
import fr.nihilus.music.inflate
import fr.nihilus.music.media.MediaItems
import fr.nihilus.music.utils.MediaID
import fr.nihilus.music.utils.MediaItemIndexer

class SongAdapter(fragment: Fragment) : BaseAdapter(), SectionIndexer {

    private val mGlideRequest: GlideRequest<Bitmap>
    private val mIndexer: MediaItemIndexer
    private val mSongs = ArrayList<MediaBrowserCompat.MediaItem>()

    init {
        mIndexer = MediaItemIndexer(mSongs)
        registerDataSetObserver(mIndexer)

        mGlideRequest = GlideApp.with(fragment).asBitmap()
                .error(R.drawable.default_selectable_album_art)
                .fitCenter()
    }

    override fun getCount() = mSongs.size

    override fun getItem(pos: Int) = mSongs[pos]

    override fun hasStableIds() = true

    override fun getItemId(pos: Int): Long {
        if (hasStableIds()) {
            val mediaId = mSongs[pos].mediaId
            return MediaID.extractMusicID(mediaId)!!.toLong()
        }
        return -1L
    }

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
        val itemView = convertView ?: createItemView(parent)

        val holder = itemView.tag as ViewHolder
        bindViewHolder(holder, pos)

        return itemView
    }

    private fun bindViewHolder(holder: ViewHolder, position: Int) {
        val item = mSongs[position]
        holder.bind(item, mGlideRequest)
    }

    private fun createItemView(parent: ViewGroup): View {
        return parent.inflate(R.layout.song_list_item, false).also {
            it.tag = ViewHolder(it)
        }
    }

    override fun getSections(): Array<out Any> = mIndexer.sections

    override fun getPositionForSection(sectionIndex: Int) =
            mIndexer.getPositionForSection(sectionIndex)

    override fun getSectionForPosition(position: Int) =
            mIndexer.getSectionForPosition(position)

    fun updateItems(newItems: List<MediaBrowserCompat.MediaItem>) {
        mSongs.clear()
        mSongs.addAll(newItems)
        notifyDataSetChanged()
    }

    private class ViewHolder(itemView: View) {
        private val mTitle: TextView = itemView.findViewById(R.id.title)
        private val mSubtitle: TextView = itemView.findViewById(R.id.subtitle)
        private val mCover: ImageView = itemView.findViewById(R.id.cover)

        fun bind(item: MediaBrowserCompat.MediaItem, glide: GlideRequest<*>) {
            with(item.description) {
                mTitle.text = title
                bindSubtitle(mSubtitle, subtitle, extras!!.getLong(MediaItems.EXTRA_DURATION))
                glide.load(iconUri).into(mCover)
            }
        }

        private fun bindSubtitle(textView: TextView, text: CharSequence?, durationMillis: Long) {
            val duration = DateUtils.formatElapsedTime(durationMillis / 1000)
            val subtitle = textView.context.getString(R.string.song_item_subtitle, text, duration)
            textView.text = subtitle
        }
    }
}
