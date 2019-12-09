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

package fr.nihilus.music.devmenu

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.roundToInt

class RangeSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs, 0, R.style.Widget_RangeSlider) {

    private val lowerBound: Float
    private val upperBound: Float
    private val step: Float

    private val trackDrawable: Drawable?
    private val progressDrawable: Drawable?
    private val thumbDrawable: Drawable?
    private val tickMark: Drawable?

    private var _lowerValue: Float
    private var _upperValue: Float

    private val leftTrackBounds = Rect()
    private val progressBounds = Rect()
    private val rightTrackBounds = Rect()

    private val lowerThumbBounds = Rect()
    private val upperThumbBounds = Rect()

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.RangeSlider, 0, R.style.Widget_RangeSlider)
        try {
            lowerBound = a.getFloat(R.styleable.RangeSlider_lowerBound, DEFAULT_LOWER_BOUND)
            upperBound = a.getFloat(R.styleable.RangeSlider_upperBound, DEFAULT_UPPER_BOUND)
                .coerceAtLeast(lowerBound)

            val defaultStep = (upperBound - lowerBound) / DEFAULT_TICK_MARKS
            val minimumStep = (upperBound - lowerBound) / MAX_TICK_MARKS
            step = a.getFloat(R.styleable.RangeSlider_step, defaultStep)
                .coerceIn(minimumStep, upperBound)

            trackDrawable = a.getDrawable(R.styleable.RangeSlider_trackDrawable)
            progressDrawable = a.getDrawable(R.styleable.RangeSlider_progressDrawable)
            thumbDrawable = a.getDrawable(R.styleable.RangeSlider_thumbDrawable)
            tickMark = a.getDrawable(R.styleable.RangeSlider_tickMarkDrawable)

            _lowerValue = a.getFloat(R.styleable.RangeSlider_lowerValue, lowerBound)
                .coerceIn(lowerBound, upperBound)
            _upperValue = a.getFloat(R.styleable.RangeSlider_upperValue, upperBound)
                .coerceIn(_lowerValue, upperBound)

        } finally {
            a.recycle()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val ww = w - paddingLeft - paddingRight
        val hh = h - paddingTop - paddingBottom

        val rangeLength = upperBound - lowerBound
        val lowerX = paddingLeft + ((ww / rangeLength) * (_lowerValue - lowerBound)).roundToInt()
        val upperX = paddingLeft + ((ww / rangeLength) * (_upperValue - lowerBound)).roundToInt()

        val medianHeight = hh / 2 + paddingTop

        if (trackDrawable != null) {
            val trackHeight = trackDrawable.intrinsicHeight

            leftTrackBounds.apply {
                left = paddingLeft
                top = medianHeight - trackHeight / 2
                right = lowerX
                bottom = medianHeight + trackHeight / 2
            }

            rightTrackBounds.apply {
                left = upperX
                top = medianHeight - trackHeight / 2
                right = w - paddingRight
                bottom = medianHeight + trackHeight / 2
            }
        }

        if (progressDrawable != null) {
            val progressHeight = progressDrawable.intrinsicHeight

            progressBounds.apply {
                left = lowerX
                top = medianHeight - progressHeight / 2
                right = upperX
                bottom = medianHeight + progressHeight / 2
            }
        }

        if (thumbDrawable != null) {
            val thumbWidth = thumbDrawable.intrinsicWidth
            val thumbHeight = thumbDrawable.intrinsicHeight

            lowerThumbBounds.apply {
                left = lowerX - thumbWidth / 2
                top = medianHeight - thumbHeight / 2
                right = lowerX + thumbWidth / 2
                bottom = medianHeight + thumbHeight / 2
            }

            upperThumbBounds.apply {
                left = upperX - thumbWidth / 2
                top = medianHeight - thumbHeight / 2
                right = upperX + thumbWidth / 2
                bottom = medianHeight + thumbHeight / 2
            }
        }
    }

    override fun getSuggestedMinimumWidth(): Int {
        val minimumWidth = super.getSuggestedMinimumWidth()
        return minimumWidth + maxOf(
            trackDrawable?.minimumWidth ?: 0,
            progressDrawable?.minimumWidth ?: 0,
            thumbDrawable?.minimumWidth ?: 0
        )
    }

    override fun getSuggestedMinimumHeight(): Int {
        val minimumHeight = super.getSuggestedMinimumHeight()
        val drawableHeight = maxOf(
            trackDrawable?.minimumHeight ?: 0,
            progressDrawable?.minimumHeight ?: 0,
            thumbDrawable?.minimumHeight ?: 0
        )

        return minimumHeight + drawableHeight
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = suggestedMinimumWidth + paddingLeft + paddingRight
        val desiredHeight = suggestedMinimumHeight + paddingTop + paddingBottom

        val measuredWidth = resolveSizeAndState(desiredWidth, widthMeasureSpec, 0)
        val measuredHeight = resolveSizeAndState(desiredHeight, heightMeasureSpec, 0)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        drawBar(canvas)
        drawTickMarks(canvas)
        drawThumbs(canvas)
    }

    private fun drawThumbs(canvas: Canvas) {
        if (thumbDrawable != null) {
            thumbDrawable.bounds = lowerThumbBounds
            thumbDrawable.draw(canvas)

            thumbDrawable.bounds = upperThumbBounds
            thumbDrawable.draw(canvas)
        }
    }

    private fun drawBar(canvas: Canvas) {
        // Inactive portion
        if (trackDrawable != null) {
            trackDrawable.bounds = leftTrackBounds
            trackDrawable.draw(canvas)

            trackDrawable.bounds = rightTrackBounds
            trackDrawable.draw(canvas)
        }

        if (progressDrawable != null) {
            progressDrawable.bounds = progressBounds
            progressDrawable.draw(canvas)
        }
    }

    private fun drawTickMarks(canvas: Canvas) {
        if (tickMark != null && !step.isNaN()) {
            val numberOfTicks = ((upperBound - lowerBound) / step).toInt()
            if (numberOfTicks > 1) {
                val w = tickMark.intrinsicWidth
                val h = tickMark.intrinsicHeight
                val halfW = if (w >= 0) w / 2 else 1
                val halfH = if (h >= 0) h / 2 else 1
                tickMark.setBounds(-halfW, -halfH, halfW, halfH)

                val spacing = (width - paddingLeft - paddingRight).toFloat() / numberOfTicks
                val saveCount = canvas.save()

                val medianHeight = paddingTop + (height - paddingTop - paddingBottom) / 2f
                canvas.translate(paddingLeft.toFloat(), medianHeight)
                repeat(numberOfTicks) {
                    tickMark.draw(canvas)
                    canvas.translate(spacing, 0f)
                }

                canvas.restoreToCount(saveCount)
            }
        }
    }
}

/**
 * The default minimum value.
 */
private const val DEFAULT_LOWER_BOUND = 0.0f

/**
 * The default maximum value.
 */
private const val DEFAULT_UPPER_BOUND = 1.0f

/**
 * The default number of tick marks to display when no step is specified.
 */
private const val DEFAULT_TICK_MARKS = 10

/**
 * The maximum number of tick marks to display when the step is too small or invalid.
 */
private const val MAX_TICK_MARKS = 20