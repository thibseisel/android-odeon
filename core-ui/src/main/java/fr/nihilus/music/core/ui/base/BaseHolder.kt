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

package fr.nihilus.music.core.ui.base

import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import fr.nihilus.music.core.ui.extensions.inflate

/**
 * @param parent The ViewGroup into which the item will be added after it is bound to
 * an adapter position.
 * @param layoutResId Identifier of the layout resource to inflate for this ViewHolder.
 */
abstract class BaseHolder<in T>(
    parent: ViewGroup,
    @LayoutRes layoutResId: Int
) : RecyclerView.ViewHolder(parent.inflate(layoutResId)) {

    /**
     * Called when the adapter requests this ViewHolder to update its view
     * to reflect the passed media item.
     * @param data The media item this ViewHolder should represent.
     */
    abstract fun bind(data: T)
}