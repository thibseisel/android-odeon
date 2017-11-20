/*
 * Copyright 2017 Thibault Seisel
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
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.RecyclerView
import fr.nihilus.music.R

class CurrentlyPlayingDecoration(
        context: Context,
        iconColor: Int
) : RecyclerView.ItemDecoration() {

    private val mIcon: Drawable
    private val mPaddingStart = context.resources.getDimensionPixelSize(R.dimen.list_item_horizontal_padding)
    private var mDecoratedPosition = RecyclerView.NO_POSITION

    init {
        val notNullIcon = checkNotNull(AppCompatResources.getDrawable(context,
                R.drawable.currently_playing_decoration)) { "Icon should not be null" }
        mIcon = DrawableCompat.wrap(notNullIcon)
        DrawableCompat.setTint(mIcon, iconColor)
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State?) {
        if (mDecoratedPosition != RecyclerView.NO_POSITION) {
            val child = parent.getChildAt(mDecoratedPosition)
            if (child != null) {
                val left = mPaddingStart
                val top = child.top + (child.height - mIcon.intrinsicHeight) / 2
                val right = left + mIcon.intrinsicWidth
                val bottom = top + mIcon.intrinsicHeight

                mIcon.setBounds(left, top, right, bottom)
                mIcon.draw(c)
            }
        }
    }

    fun setDecoratedItemPosition(position: Int) {
        mDecoratedPosition = position
    }
}
