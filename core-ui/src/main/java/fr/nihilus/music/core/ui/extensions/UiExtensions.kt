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

package fr.nihilus.music.core.ui.extensions

import android.content.Context
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.Window
import androidx.annotation.Dimension
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
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
 * Whether the window this View belongs to has requested to be drawn behind the status bar
 * and the navigation bar.
 */
var View.isDrawnEdgeToEdge: Boolean
    get() = (systemUiVisibility and View.SYSTEM_UI_FLAG_LAYOUT_STABLE != 0) &&
            (systemUiVisibility and View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION != 0)
    set(value) {
        systemUiVisibility = when (value) {
            true -> systemUiVisibility or (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
            else -> systemUiVisibility and (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION).inv()
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
@Suppress("unused")
fun dipToPixels(context: Context, @Dimension(unit = Dimension.DP) dp: Float): Int {
    val metrics = context.resources.displayMetrics
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics).roundToInt()
}

/**
 * Helper function for starting the action mode from a Fragment.
 * This function uses the compatibility action mode.
 *
 * @param callback Callback that will manage lifecycle events for this context mode.
 * @return The action mode that was started, or `null` if it was canceled.
 */
fun Fragment.startActionMode(callback: ActionMode.Callback): ActionMode? {
    val hostActivity = activity as? AppCompatActivity
        ?: error(
            "Starting the action mode requires the calling fragment " +
                    "to be attached to a subclass of AppCompatActivity"
        )
    return hostActivity.startSupportActionMode(callback)
}