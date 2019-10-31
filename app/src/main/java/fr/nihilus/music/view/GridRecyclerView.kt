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

package fr.nihilus.music.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.GridLayoutAnimationController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView with support for grid animations.
 * Grid layout animations are defined as anim resources with a "<gridLayoutAnimation>" tag.
 * and are used with the "android:layoutAnimation" attribute.
 */
class GridRecyclerView : RecyclerView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun attachLayoutAnimationParameters(child: View, params: ViewGroup.LayoutParams, index: Int, count: Int) {
        val layoutManager = layoutManager
        val adapter = adapter

        if (adapter != null && layoutManager is GridLayoutManager) {
            // If there are no animation parameters, create new ones and attach them to the LayoutParams.
            val animationParams = params.layoutAnimationParameters as GridLayoutAnimationController.AnimationParameters?
                ?: GridLayoutAnimationController.AnimationParameters().also {
                    params.layoutAnimationParameters = it
                }

            // Set the number of items in the RecyclerView and the index of this item.
            animationParams.count = count
            animationParams.index = index

            // Calculate the number of columns and rows in the grid.
            val columns = layoutManager.spanCount
            animationParams.columnsCount = columns

            // Because GridLayoutManager allows items to take more than 1 span,
            // we need to inspect the SpanSizeLookup to find the row of the last visible element.
            val spanLookup = layoutManager.spanSizeLookup
            val lastRowIndex = spanLookup.getSpanGroupIndex(count, columns)
            animationParams.rowsCount = lastRowIndex + 1

            // The same applies when calculating the view's row and column.
            // TODO This lacks support for stackFromEnd/reverseLayout = true
            animationParams.column = spanLookup.getSpanIndex(index, columns)
            animationParams.row = spanLookup.getSpanGroupIndex(index, columns)

        } else {
            // Proceed as normal if using another type of LayoutManager.
            super.attachLayoutAnimationParameters(child, params, index, count)
        }
    }
}