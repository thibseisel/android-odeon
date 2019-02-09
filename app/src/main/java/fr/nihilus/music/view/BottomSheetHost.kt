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
import android.widget.FrameLayout

/**
 * A custom [FrameLayout] that is intended to be used as the main container
 * for the content of a bottom sheet.
 * This prevents from accidentally clicking views that are hidden behind the bottom sheet when it is expanded.
 */
class BottomSheetHost : FrameLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        // Prevent from dispatching touch events to views behind the Bottom Sheet.
        isFocusable = true
        isClickable = true
    }

    override fun dispatchSetPressed(pressed: Boolean) {
        // Do not dispatch pressed state to children.
    }

    override fun dispatchSetActivated(activated: Boolean) {
        // Do not dispatch activated state to children.
    }
}