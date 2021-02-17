/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.nihilus.music.core.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.nihilus.music.core.ui.R
import fr.nihilus.music.core.ui.view.DividerItemDecoration.Companion.HORIZONTAL
import fr.nihilus.music.core.ui.view.DividerItemDecoration.Companion.VERTICAL
import kotlin.math.roundToInt

/**
 * DividerItemDecoration is a [RecyclerView.ItemDecoration] that can be used as a divider
 * between items of a [LinearLayoutManager].
 * It supports both [HORIZONTAL] and [VERTICAL] orientations.
 *
 * Unlike the built-in implementation, this decoration won't draw a divider for the list item.
 *
 * ```
 * dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), layoutManager.getOrientation());
 * recyclerView.addItemDecoration(dividerItemDecoration);
 * ```
 *
 * @constructor
 * Creates a divider [RecyclerView.ItemDecoration] that can be used with a [LinearLayoutManager].
 * @param context Current context, it will be used to access resources.
 * @param orientation Divider orientation. Should be [HORIZONTAL] or [VERTICAL].
 */
class DividerItemDecoration(
    context: Context,
    val orientation: Int
) : RecyclerView.ItemDecoration() {

    companion object {
        const val HORIZONTAL = LinearLayout.HORIZONTAL
        const val VERTICAL = LinearLayout.VERTICAL
        private val DIVIDER_THEME_ATTRS = intArrayOf(
            android.R.attr.listDivider,
            R.attr.dividerHorizontal,
            R.attr.dividerVertical
        )
    }

    private val bounds = Rect()

    /**
     * The [Drawable] for this divider.
     */
    val drawable: Drawable

    init {
        require(orientation == VERTICAL || orientation == HORIZONTAL) {
            "Invalid orientation. It should be either HORIZONTAL or VERTICAL"
        }

        val a = context.obtainStyledAttributes(DIVIDER_THEME_ATTRS)
        try {
            @SuppressLint("ResourceType")
            val dividerDrawable = when (orientation) {
                VERTICAL -> a.getDrawable(2)
                else -> a.getDrawable(1)
            } ?: a.getDrawable(0)

            drawable = requireNotNull(dividerDrawable) {
                "One of attr/dividerHorizontal, attr/dividerVertical, android:attr/listDivider " +
                        "should be defined in the theme used by this DividerItemDecoration"
            }

        } finally {
            a.recycle()
        }
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        if (orientation == VERTICAL) {
            outRect.set(0, 0, 0, drawable.intrinsicHeight)
        } else {
            outRect.set(0, 0, drawable.intrinsicWidth, 0)
        }
    }

    override fun onDraw(
        c: Canvas,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        if (parent.layoutManager == null) return
        if (orientation == VERTICAL) {
            drawVertical(c, parent)
        } else {
            drawHorizontal(c, parent)
        }
    }

    private fun drawVertical(
        canvas: Canvas,
        parent: RecyclerView
    ) {
        canvas.save()
        val left: Int
        val right: Int
        if (parent.clipToPadding) {
            left = parent.paddingLeft
            right = parent.width - parent.paddingRight
            canvas.clipRect(left, parent.paddingTop, right, parent.height - parent.paddingBottom)
        } else {
            left = 0
            right = parent.width
        }
        val childCount = parent.childCount
        for (i in 0 until childCount - 1) {
            val child = parent.getChildAt(i)
            parent.getDecoratedBoundsWithMargins(child, bounds)
            val bottom = bounds.bottom + child.translationY.roundToInt()
            val top = bottom - drawable.intrinsicHeight
            drawable.setBounds(left, top, right, bottom)
            drawable.draw(canvas)
        }
        canvas.restore()
    }

    private fun drawHorizontal(
        canvas: Canvas,
        parent: RecyclerView
    ) {
        canvas.save()
        val top: Int
        val bottom: Int
        if (parent.clipToPadding) {
            top = parent.paddingTop
            bottom = parent.height - parent.paddingBottom
            canvas.clipRect(
                parent.paddingLeft, top,
                parent.width - parent.paddingRight, bottom
            )
        } else {
            top = 0
            bottom = parent.height
        }
        val childCount = parent.childCount
        for (i in 0 until childCount - 1) {
            val child = parent.getChildAt(i)
            parent.layoutManager!!.getDecoratedBoundsWithMargins(child, bounds)
            val right = bounds.right + child.translationX.roundToInt()
            val left = right - drawable.intrinsicWidth
            drawable.setBounds(left, top, right, bottom)
            drawable.draw(canvas)
        }
        canvas.restore()
    }
}