/*
 * Copyright 2020 Thibault Seisel
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

package fr.nihilus.music.devmenu

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import fr.nihilus.music.core.ui.extensions.inflate
import fr.nihilus.music.devmenu.StaticAdapter.Row

/**
 * A [RecyclerView.Adapter] implementation that have a pre-defined set of rows.
 *
 * @param rows The list of rows to be displayed by the adapter.
 * Every row should have a unique [Row.id].
 * Future changes to the content of the list will _not_ refresh the adapter.
 * @property listener A function to be called when a row is clicked.
 */
internal class StaticAdapter(
    rows: List<Row>,
    private val listener: (Row) -> Unit
) : RecyclerView.Adapter<StaticAdapter.RowHolder>() {

    private val rows: List<Row> = rows.toList()

    init {
        setHasStableIds(true)
        require(rows.size == rows.distinctBy(Row::id).size) {
            "Every row should have a unique identifier."
        }
    }

    override fun getItemCount(): Int = rows.size

    override fun getItemId(position: Int): Long = position.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = RowHolder(parent, listener)

    override fun onBindViewHolder(holder: RowHolder, position: Int) {
        holder.bind(rows[position])
    }

    inner class RowHolder(
        parent: ViewGroup,
        listener: (Row) -> Unit
    ) : RecyclerView.ViewHolder(parent.inflate(R.layout.dev_single_line_list_item)) {

        private val icon: ImageView = itemView.findViewById(R.id.icon)
        private val title: TextView = itemView.findViewById(R.id.title)

        init {
            itemView.setOnClickListener {
                val selectedRow = rows[adapterPosition]
                listener(selectedRow)
            }
        }

        fun bind(model: Row) {
            icon.isVisible = model.iconRes != 0
            icon.setImageResource(model.iconRes)

            title.setText(model.titleRes)
        }
    }

    /**
     * Model associated with the representation of a row in [StaticAdapter].
     *
     * @property id An unique identifier for the row.
     * @property titleRes A string resource of the text to be used as this item's title.
     * @property iconRes Optional drawable resource to be used as this item's icon.
     * May be `0` to display no icon.
     */
    class Row(
        @IdRes val id: Int,
        @StringRes val titleRes: Int,
        @DrawableRes val iconRes: Int = 0
    )
}