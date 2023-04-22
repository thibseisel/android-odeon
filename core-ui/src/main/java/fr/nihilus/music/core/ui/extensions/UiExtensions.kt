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
import android.view.ViewGroup
import android.view.Window
import androidx.annotation.Dimension
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import kotlin.math.roundToInt

/**
 * Whether dark icons are displayed over the status bar for the given [Window].
 * This is `true` for dark icons and `false` for white ones.
 */
@get:RequiresApi(Build.VERSION_CODES.M)
@set:RequiresApi(Build.VERSION_CODES.M)
var Window.darkSystemIcons: Boolean
    get() {
        val controller = requireDecorInsetsController()
        return controller.isAppearanceLightStatusBars
    }
    set(areDarkIcons) {
        val controller = requireDecorInsetsController()
        controller.isAppearanceLightStatusBars = areDarkIcons
    }

private fun Window.requireDecorInsetsController(): WindowInsetsControllerCompat =
    WindowCompat.getInsetsController(this, decorView)

/**
 * Sets whether this window has requested to be drawn behind the status bar and the navigation bar.
 * @param edgeToEdge `true` to draw window edge to edge.
 */
fun Window.drawEdgeToEdge(edgeToEdge: Boolean) {
    WindowCompat.setDecorFitsSystemWindows(this, !edgeToEdge)
}

/**
 * Packed offset dimensions applied to a View, such as padding or margin.
 *
 * @property left Spacing applied at left, in pixels.
 * @property top Spacing applied on top, in pixels.
 * @property right Spacing applied at right, in pixels.
 * @property bottom Spacing applied below, in pixels.
 */
class ViewSpacing(@Px val left: Int, @Px val top: Int, @Px val right: Int, @Px val bottom: Int)

/**
 * Allow applying window insets to a view, preventing this view to be obscured
 * by system UI portions such as the status bar and the navigation bar.
 *
 * @param block A function to be called when the window should apply its insets.
 * That function is passed the target view, the window insets and the view's initial padding
 * and margin.
 */
fun View.doOnApplyWindowInsets(
    block: (View, insets: WindowInsetsCompat, padding: ViewSpacing, margin: ViewSpacing) -> Unit
) {
    val initialPadding = ViewSpacing(paddingStart, paddingTop, paddingEnd, paddingBottom)
    val initialMargin = with(layoutParams as ViewGroup.MarginLayoutParams) {
        ViewSpacing(leftMargin, topMargin, rightMargin, bottomMargin)
    }

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        block(view, insets, initialPadding, initialMargin)
        insets
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

/**
 * Start enter transitions that were postponed for this fragment when its content has been redrawn.
 * This is meant to be used when the data backing a RecyclerView
 * has been updated for the first time.
 *
 * See [https://developer.android.com/training/basics/fragments/animate#recyclerview]
 */
fun Fragment.startPostponedEnterTransitionWhenDrawn() {
    (requireView().parent as? ViewGroup)?.doOnPreDraw {
        startPostponedEnterTransition()
    }
}
