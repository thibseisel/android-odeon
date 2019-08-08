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

package fr.nihilus.music.library.cleanup

import androidx.recyclerview.widget.DiffUtil

/**
 * A wrapper for an item that can selected from the UI.
 */
class Checkable<out T : Any>(
    val item: T,
    var isChecked: Boolean
)

/**
 * Define how items wrapped in [Checkable] should be compared.
 * This delegates to the provided [delegateCallback].
 *
 * @param T The type of the item wrapped in [Checkable].
 * @param delegateCallback Callback for calculating the differences between wrapped items.
 */
class CheckableDiffer<T : Any>(
    private val delegateCallback: DiffUtil.ItemCallback<T>
) : DiffUtil.ItemCallback<Checkable<T>>() {

    override fun areItemsTheSame(oldItem: Checkable<T>, newItem: Checkable<T>): Boolean =
        delegateCallback.areItemsTheSame(oldItem.item, newItem.item)

    override fun areContentsTheSame(oldItem: Checkable<T>, newItem: Checkable<T>): Boolean =
        delegateCallback.areContentsTheSame(oldItem.item, newItem.item)

    override fun getChangePayload(oldItem: Checkable<T>, newItem: Checkable<T>): Any? =
        delegateCallback.getChangePayload(oldItem.item, newItem.item)
}