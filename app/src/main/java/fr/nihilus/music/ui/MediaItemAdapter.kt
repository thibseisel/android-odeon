package fr.nihilus.music.ui

import android.support.v4.media.MediaBrowserCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import fr.nihilus.music.di.FragmentScoped
import fr.nihilus.music.utils.MediaItemDiffCallback
import javax.inject.Inject

@FragmentScoped
class MediaItemAdapter
@Inject constructor(
        val factory: ViewHolderFactory
) : RecyclerView.Adapter<MediaItemHolder>() {

    private val mItems: MutableList<MediaBrowserCompat.MediaItem> = ArrayList()

    override fun getItemCount() = mItems.size

    override fun getItemViewType(position: Int): Int {
        val item = mItems[position]
        return factory.viewTypeFor(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            factory.create(parent, viewType)

    override fun onBindViewHolder(holder: MediaItemHolder, position: Int) {
        val item = mItems[position]
        holder.bind(item)
    }

    fun setItems(newItems: List<MediaBrowserCompat.MediaItem>) {
        val callback = MediaItemDiffCallback(mItems, newItems)
        val result = DiffUtil.calculateDiff(callback, false)
        mItems.clear()
        mItems.addAll(newItems)
        result.dispatchUpdatesTo(this)
    }
}