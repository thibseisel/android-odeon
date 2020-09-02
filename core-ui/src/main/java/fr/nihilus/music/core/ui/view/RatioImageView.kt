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

package fr.nihilus.music.core.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup.LayoutParams
import androidx.annotation.IntDef
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.roundToInt
import fr.nihilus.music.core.ui.R

/**
 * An ImageView whose dimensions can be constrained by an aspect ratio.
 *
 * Aspect ratio can be configured through [setAspectRatio], [setAspectRatioSide]
 * and their equivalent XML attributes `aspectRatio` and `aspectRatioSide`.
 *
 * Note that aspect ratio is only respected when this parent's view does not impose a specific size,
 * i.e. the calculated dimension is [LayoutParams.WRAP_CONTENT].
 */
class RatioImageView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    companion object {
        /**
         * Use width as the base dimension to calculate the height.
         * This is the default.
         */
        const val RATIO_SIDE_WIDTH = 0
        /**
         * Use height as the base dimension to calculate the width.
         */
        const val RATIO_SIDE_HEIGHT = 1
    }

    /**
     * Denotes that the annotated element represents the dimension to be used as a base
     * to calculate the other.
     */
    @MustBeDocumented
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(value = [RATIO_SIDE_WIDTH, RATIO_SIDE_HEIGHT])
    annotation class AspectRatioSide

    @AspectRatioSide
    private var ratioSide: Int = RATIO_SIDE_WIDTH

    private var aspectRatio: Float = Float.NaN

    init {
        if (attrs != null) {
            val a = context.theme.obtainStyledAttributes(attrs, R.styleable.RatioImageView, defStyleAttr, 0)
            try {
                val stringRatio = a.getString(R.styleable.RatioImageView_aspectRatio)
                aspectRatio = parseRatio(stringRatio)
                ratioSide = a.getInt(R.styleable.RatioImageView_aspectRatioSide, RATIO_SIDE_WIDTH)

            } finally {
                a.recycle()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (aspectRatio.isNaN()) {
            // Aspect ratio is disabled.
            return
        }

        val newMeasuredWidth: Int
        val newMeasuredHeight: Int

        if (ratioSide == RATIO_SIDE_HEIGHT) {
            newMeasuredHeight = measuredHeight
            newMeasuredWidth = applyRatio(newMeasuredHeight, widthMeasureSpec)
        } else {
            newMeasuredWidth = measuredWidth
            newMeasuredHeight = applyRatio(newMeasuredWidth, heightMeasureSpec)
        }

        setMeasuredDimension(newMeasuredWidth, newMeasuredHeight)
    }

    /**
     * Calculate the size of a dimension based on the other through aspect ratio.
     * The calculated size still satisfies measurement specifications.
     *
     * @param baseDimension The size in pixels of the base dimension.
     * @param measureSpec The measurement specifications for the dimension to be calculated
     * encoded with [View.MeasureSpec].
     * @return The measured size of the desired dimension with aspect ratio applied, in pixels.
     */
    @SuppressLint("SwitchIntDef")
    private fun applyRatio(baseDimension: Int, measureSpec: Int): Int {
        val desiredSize = (baseDimension / aspectRatio).roundToInt()

        // Ratio is respected only if it satisfies the measureSpec
        return when (MeasureSpec.getMode(measureSpec)) {
            MeasureSpec.UNSPECIFIED -> desiredSize
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(measureSpec)
            else -> minOf(desiredSize, MeasureSpec.getSize(measureSpec))
        }
    }

    /**
     * Changes the aspect ratio, expressed as the result of the division of
     * the base dimension by the other.
     * @param ratio A positive, finite number or [Float.NaN] to disable aspect ratio.
     */
    fun setAspectRatio(ratio: Float) {
        aspectRatio = if (ratio.isFinite() && ratio > 0f) ratio else Float.NaN
        requestLayout()
    }

    /**
     * Changes the aspect ratio.
     *
     * The aspect ratio string accepts the following formats:
     * - `N:D` where `N` is the nominator and `D` denominator (fraction form)
     * - `R` where R is a number (decimal form)
     *
     * Any invalid format will disable aspect ratio.
     *
     * @param ratio The desired aspect ratio, or `null` to disable aspect ratio.
     */
    fun setAspectRatio(ratio: String?) {
        aspectRatio = parseRatio(ratio)
        requestLayout()
    }

    /**
     * Sets the base dimension to be used to calculate the other.
     * This is the width by default.
     * Any invalid value will set the base dimension to [RATIO_SIDE_WIDTH].
     *
     * @param side One of the constants [RATIO_SIDE_WIDTH] or [RATIO_SIDE_HEIGHT].
     */
    fun setAspectRatioSide(@AspectRatioSide side: Int) {
        ratioSide = if (side != RATIO_SIDE_HEIGHT) RATIO_SIDE_WIDTH else side
        requestLayout()
    }

    /**
     * Parse the aspectRatio string property to a number.
     * The dimension string accepts the following formats:
     * - `W:H` where `W` is width and `H` the height (fraction form)
     * - `R` where R is the result of width / height (decimal form)
     *
     * @param dimensionRatio The dimension ratio encoded as a String.
     * Must follow one of the formats described above.
     * @return The decoded aspect ratio, or [Float.NaN] if input is incorrect.
     */
    private fun parseRatio(dimensionRatio: String?): Float {

        if (dimensionRatio != null) {
            val colonIndex = dimensionRatio.indexOf(':')
            if (colonIndex in dimensionRatio.indices) {
                // "width:height" format. Separate nominator and denominator.
                val nominator = dimensionRatio.substring(0, colonIndex)
                val denominator = dimensionRatio.substring(colonIndex + 1)

                if (nominator.isNotEmpty() && denominator.isNotEmpty()) {
                    try {
                        val nominatorValue = nominator.toFloat()
                        val denominatorValue = denominator.toFloat()

                        if (nominatorValue > 0f && denominatorValue > 0f) {
                            return nominatorValue / denominatorValue
                        }

                    } catch (nfe: NumberFormatException) {
                        // Ignored format exception
                    }
                }
            } else if (dimensionRatio.isNotEmpty()) {
                // "ratio" format. Convert to float if possible.
                try {
                    val ratio = dimensionRatio.toFloat()
                    return if (ratio > 0f) ratio else Float.NaN
                } catch (nfe: NumberFormatException) {
                    // Ignored format exception
                }
            }
        }

        // Disable aspect ratio when String cannot be parsed
        return Float.NaN
    }
}