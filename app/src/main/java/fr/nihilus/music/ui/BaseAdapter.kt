/*
 * Copyright 2018 Thibault Seisel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.nihilus.music.ui

import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.nihilus.music.core.ui.extensions.inflate

/**
 * A RecyclerView adapter dedicated to the display of a set of media items.
 */
abstract class BaseAdapter<VH : BaseAdapter.ViewHolder> : ListAdapter<MediaItem, VH>(MediaItemDiffer) {

    /**
     * Called when RecyclerView needs a new ViewHolder of the given type to represent an item.
     *
     * Implementations may also want to propagate view events back to this adapter's client.
     * In such case you should call [ViewHolder.onAttachListeners] on the newly created
     * ViewHolder to let it configure its view event listeners.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to
     * an adapter position.
     * @param viewType The view type of the new View.
     *
     * @return A new ViewHolder that holds a View of the given view type.
     */
    abstract override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH

    final override fun onBindViewHolder(holder: VH, position: Int) {
        holder.onBind(this.getItem(position))
    }

    public override fun getItem(position: Int): MediaItem = super.getItem(position)

    /**
     * An extension of ViewHolder designed to display media items.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to
     * an adapter position.
     * @param layoutResId Identifier of the layout to inflate for this ViewHolder.
     */
    abstract class ViewHolder(
        parent: ViewGroup,
        @LayoutRes layoutResId: Int
    ) : RecyclerView.ViewHolder(parent.inflate(layoutResId)) {

        /**
         * Called when this ViewHolder needs to configure view events to which it responds.
         * Every event should be forwarded to the passed client listener with the adapter
         * position of this ViewHolder and a code describing the action clients should perform.
         *
         * @param client The clients listener to which forward view events.
         */
        abstract fun onAttachListeners(client: OnItemSelectedListener)

        /**
         * Called when the adapter requests this ViewHolder to update its view
         * to reflect the passed media item.
         * @param item The media item this ViewHolder should represent.
         */
        abstract fun onBind(item: MediaItem)
    }

    /**
     * Notify for item selection events in RecyclerView.
     * This callback is used by adapters to forward events to their containing fragment.
     */
    interface OnItemSelectedListener {

        /**
         * Called when an item from an adapter is selected.
         *
         * @param position The position of the selected item in the adapter.
         * You may retrieve a reference to this item using [BaseAdapter.getItem].
         * @param actionId A unique code describing the action clients should trigger
         * as a result for selecting this item.
         */
        fun onItemSelected(position: Int, actionId: Int)
    }
}