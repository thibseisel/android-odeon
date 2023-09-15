/*
 * Copyright 2019 Thibault Seisel
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

package fr.nihilus.music.core.ui.base

import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import androidx.annotation.LayoutRes
import fr.nihilus.music.core.ui.base.ListAdapter.ViewHolder
import fr.nihilus.music.core.ui.extensions.inflate

/**
 * A base implementation of [android.widget.ListAdapter] backed by a list of items
 * that uses the [ViewHolder] pattern to recycle view references.
 *
 * Its API mimics that of [RecyclerView.Adapter][androidx.recyclerview.widget.RecyclerView.Adapter]
 * to make it easier to migrate implementations to support recycler view at a later time.
 */
abstract class ListAdapter<T, VH : ViewHolder> : BaseAdapter() {
    private val _items = mutableListOf<T>()

    /**
     * A live view of items currently represented by this adapter.
     */
    protected val items: List<T>
        get() = _items

    /**
     * Called when the AdapterView needs a new [ViewHolder] of the given [type][viewType]
     * to represent an item.
     *
     * This new ViewHolder should be constructed with a new View that can represent the items
     * of the given type. You can either create a new View manually or inflate it from an XML
     * layout file.
     *
     * The new ViewHolder will be used to display items of the adapter using [onBindViewHolder].
     * Since it will be re-used to display different items in the data set, it is a good idea
     * to cache references to sub views of the View to avoid unnecessary [View.findViewById] calls.
     *
     * @param parent The ViewGroup into which the new View will be added
     * after it is bound to an adapter position.
     * @param viewType The view type of the new View, as per [getItemViewType].
     */
    abstract fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH

    /**
     * Called by AdapterView to display the data at the specified position.
     * This method should update the contents of the [ViewHolder.itemView]
     * to reflect the item at the given position.
     *
     * @param holder The ViewHolder which should be updated
     * to represent the contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    abstract fun onBindViewHolder(holder: VH, position: Int)

    final override fun getCount(): Int = items.size

    final override fun getItem(position: Int): T {
        if (position in _items.indices) {
            return _items[position]
        } else throw IndexOutOfBoundsException()
    }

    /**
     * Submit a new list to be displayed, overwriting the current list if any.
     * The AdapterView will be notified of a change of data and will update its view.
     *
     * @param newList The new list containing items to be displayed.
     */
    open fun submitList(newList: List<T>) {
        _items.clear()
        _items += newList
        notifyDataSetChanged()
    }

    /**
     * Get the row id associated with the specified position in the list.
     * This return `no ID` by default.
     */
    override fun getItemId(position: Int): Long = -1L

    final override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder = if (convertView == null) {
            val viewType = getItemViewType(position)
            createViewHolder(parent, viewType)
        } else {
            @Suppress("UNCHECKED_CAST")
            convertView.tag as VH
        }

        bindViewHolder(holder, position)
        return holder.itemView
    }

    private fun createViewHolder(parent: ViewGroup, viewType: Int): VH {
        val holder = onCreateViewHolder(parent, viewType)
        check(holder.itemView.parent == null) {
            "ViewHolder views must not be attached when created."
        }

        holder.itemView.tag = holder
        holder.itemViewType = viewType
        return holder
    }

    private fun bindViewHolder(holder: VH, position: Int) {
        holder.position = position
        onBindViewHolder(holder, position)
    }

    /**
     * A ViewHolder describes an item view and metadata about its place within the AdapterView.
     *
     * [ListAdapter] implementations should subclass [ViewHolder] and add fields
     * for caching potentially expensive [View.findViewById] results.
     */
    abstract class ViewHolder
    protected constructor(val itemView: View) {

        /**
         * Inflating the [itemView] from a specified [layout resource][resId].
         *
         * @param parent The parent view that the item view would be eventually attached to.
         * @param resId The identifier of a layout resource that represents the item view.
         */
        protected constructor(
            parent: ViewGroup,
            @LayoutRes resId: Int
        ) : this(parent.inflate(resId))

        /**
         * The view type of this ViewHolder.
         */
        var itemViewType: Int = ListView.ITEM_VIEW_TYPE_IGNORE
            internal set

        /**
         * The adapter position of the item if it still exists in the adapter.
         */
        var position: Int = ListView.INVALID_POSITION
            internal set
    }
}
