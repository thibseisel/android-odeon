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

package fr.nihilus.music.ui

import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter

abstract class ListAdapter<T, VH : ListAdapter.ViewHolder> : BaseAdapter() {
    private val items = mutableListOf<T>()

    abstract fun onCreateViewHolder(container: ViewGroup): VH

    abstract fun onBindViewHolder(holder: VH, position: Int)

    final override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder = convertView?.tag as? VH ?: onCreateViewHolder(parent).also {
            it.itemView.tag = it
        }

        onBindViewHolder(holder, position)
        return holder.itemView
    }

    final override fun getItem(position: Int): T {
        if (position in items.indices) {
            return items[position]
        } else throw IndexOutOfBoundsException()
    }

    override fun getItemId(position: Int): Long = -1L

    final override fun getCount(): Int = items.size

    fun submitList(newItems: List<T>) {
        items.clear()
        items += newItems
        notifyDataSetChanged()
    }

    abstract class ViewHolder(val itemView: View)
}