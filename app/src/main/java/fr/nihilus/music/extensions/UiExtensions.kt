/*
 * Copyright 2018 Thibault Seisel
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

package fr.nihilus.music.extensions

import android.content.Context
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.Window
import androidx.annotation.Dimension
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import kotlin.math.roundToInt

/**
 * Whether dark icons are displayed over the status bar for the given [Window].
 * This is `true` for dark icons and `false` for white ones.
 */
@get:RequiresApi(Build.VERSION_CODES.M)
@set:RequiresApi(Build.VERSION_CODES.M)
var Window.darkSystemIcons: Boolean
    get() = decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR != 0
    set(areDarkIcons) = with(decorView) {
        systemUiVisibility = if (areDarkIcons) {
            systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
    }

/**
 * Convert Density-Independent Pixels to raw pixels using device information
 * provided by the [context].
 *
 * @param dp Dimension to convert to raw pixels expressed in DIP.
 * @return The corresponding dimension in raw pixels.
 */
@Px
fun dipToPixels(context: Context, @Dimension(unit = Dimension.DP) dp: Float): Int {
    val metrics = context.resources.displayMetrics
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics).roundToInt()
}