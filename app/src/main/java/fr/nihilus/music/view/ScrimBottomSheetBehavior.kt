package fr.nihilus.music.view

import android.content.Context
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.CoordinatorLayout
import android.util.AttributeSet
import android.view.View

/**
 * A BottomSheet Behavior implementation that draws a semi-transparent black rectangle
 * behind the BottomSheet. This rectangle is fully transparent when hidden or collapsed
 * and becomes gradually opaque as it expands.
 *
 * It is the client responsibility to set the opacity of the scrim rectangle when the BottomSheet
 * slides. For this purpose, you can use [BottomSheetBehavior.BottomSheetCallback.onSlide].
 */
internal class ScrimBottomSheetBehavior<V : View>(context: Context?, attrs: AttributeSet?)
    : BottomSheetBehavior<V>(context, attrs) {

    /**
     * The opacity of the scrim behind the BottomSheet.
     */
    internal var scrimOpacity: Float = 0.0f

    override fun getScrimOpacity(parent: CoordinatorLayout?, child: V) = scrimOpacity

    override fun blocksInteractionBelow(parent: CoordinatorLayout?, child: V) = true

    companion object {
        @JvmStatic fun <V : View> from(view: V): ScrimBottomSheetBehavior<V> {
            val params = view.layoutParams as? CoordinatorLayout.LayoutParams
                    ?: throw IllegalArgumentException("The view is not a child of CoordinatorLayout")
            val behavior = params
                    .behavior as? ScrimBottomSheetBehavior<*> ?: throw IllegalArgumentException(
                    "The view is not associated with BottomSheetBehavior")

            @Suppress("UNCHECKED_CAST")
            return behavior as ScrimBottomSheetBehavior<V>
        }
    }
}