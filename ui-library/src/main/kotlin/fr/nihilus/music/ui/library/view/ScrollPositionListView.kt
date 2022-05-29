/*
 * Copyright 2022 Thibault Seisel
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

package fr.nihilus.music.ui.library.view

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.widget.ListView

/**
 * A ListView that saves and restores its scrolling position when a configuration change occurs.
 */
internal class ScrollPositionListView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ListView(context, attrs, android.R.attr.listViewStyle) {

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        return SavedState(superState).apply {
            scrollPosition = firstVisiblePosition
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)
        val desiredPosition = state.scrollPosition
        if (desiredPosition < count) {
            setSelectionFromTop(desiredPosition, 0)
        }
    }

    private class SavedState : BaseSavedState {
        @JvmField var scrollPosition: Int = 0

        constructor(superState: Parcelable?) : super(superState)

        constructor(source: Parcel) : super(source) {
            scrollPosition = source.readInt()
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(source: Parcel) = SavedState(source)
            override fun newArray(size: Int) = arrayOfNulls<SavedState>(size)
        }
    }
}
