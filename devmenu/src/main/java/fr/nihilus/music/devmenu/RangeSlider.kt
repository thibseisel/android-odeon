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
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.Region
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.core.graphics.withSave

class RangeSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs, 0, R.style.Widget_RangeSlider) {

    private var _minValue: Float = DEFAULT_MIN_VALUE
    private var _maxValue: Float = DEFAULT_MAX_VALUE
    private val step: Float

    private val track: Drawable?
    private val activeTrack: Drawable?
    private val thumb: Drawable?
    private val tickMark: Drawable?

    private var _lowerValue: Float = _minValue
    private var _upperValue: Float = _maxValue

    private val activeBounds = Rect()

    private val lowerThumbBounds = Rect()
    private val upperThumbBounds = Rect()

    /**
     * Distance in pixels a touch can wander before we think the user is scrolling.
     */
    private val scaledTouchSlop: Int

    private var rangeListener: OnRangeChangedListener? = null

    /**
     * The minimum value of the range.
     * Defaults to `0.0` if not specified.
     *
     * @see R.styleable.RangeSlider_minValue
     */
    var minValue: Float
        get() = _minValue
        set(value) {
            _minValue = value
            invalidate()
        }

    /**
     * The maximum value of the range.
     * This should be greater than or equal to [minValue], and is clamped otherwise.
     * Defaults to 1.0 if not specified.
     *
     * @see R.styleable.RangeSlider_maxValue
     */
    var maxValue: Float
        get() = _maxValue
        set(value) {
            _maxValue = value.coerceAtLeast(_minValue)
            invalidate()
        }

    /**
     * The lower value of the range.
     * This value should be included in the `[minValue ; maxValue]` interval and is clamped otherwise.
     * Defaults to [minValue] if not specified.
     *
     * @see R.styleable.RangeSlider_lowerBound
     */
    var lowerBound: Float
        get() = _lowerValue
        set(value) {
            _lowerValue = value.coerceIn(_minValue, _maxValue)
            val paddedWidth = width - paddingLeft - paddingRight
            val lowerScale = scaled(_lowerValue)
            activeBounds.left = (paddedWidth * lowerScale).toInt()

            invalidate()
        }

    /**
     * The upper value of the range.
     * This value should be included in the [lowerBound ; maxValue] interval and is clamped otherwise.
     * Defaults to [maxValue] if not specified.
     *
     * @see R.styleable.RangeSlider_upperBound
     */
    var upperBound: Float
        get() = _upperValue
        set(value) {
            _upperValue = value.coerceIn(_lowerValue, _maxValue)
            val paddedWidth = width - paddingLeft - paddingRight
            val upperScale = scaled(_upperValue)
            activeBounds.right = (paddedWidth * upperScale).toInt()

            invalidate()
        }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.RangeSlider, 0, R.style.Widget_RangeSlider)
        try {
            // Retrieve the drawable components used for the appearance of the slider.
            track = a.getDrawable(R.styleable.RangeSlider_trackDrawable)
            activeTrack = a.getDrawable(R.styleable.RangeSlider_progressDrawable)
            thumb = a.getDrawable(R.styleable.RangeSlider_thumbDrawable)
            tickMark = a.getDrawable(R.styleable.RangeSlider_tickMarkDrawable)

            // Retrieve the extreme values for the widest range.
            minValue = a.getFloat(R.styleable.RangeSlider_minValue, DEFAULT_MIN_VALUE)
            maxValue = a.getFloat(R.styleable.RangeSlider_maxValue, DEFAULT_MAX_VALUE)

            val minimumStep = (_maxValue - _minValue) / MAX_TICK_MARKS
            step = a.getFloat(R.styleable.RangeSlider_step, STEP_CONTINUOUS)
                .coerceIn(minimumStep, _maxValue)

            // Sets the initial range.
            lowerBound = a.getFloat(R.styleable.RangeSlider_lowerBound, _minValue)
            upperBound = a.getFloat(R.styleable.RangeSlider_upperBound, _maxValue)

            isEnabled = a.getBoolean(R.styleable.RangeSlider_android_enabled, true)

        } finally {
            a.recycle()
        }

        scaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
    }

    override fun getSuggestedMinimumWidth(): Int {
        val minimumWidth = super.getSuggestedMinimumWidth()
        val trackWidth = minimumWidth + maxOf(
            track?.minimumWidth ?: 0,
            activeTrack?.minimumWidth ?: 0
        )

        return maxOf(minimumWidth, trackWidth)
    }

    override fun getSuggestedMinimumHeight(): Int {
        val minimumHeight = super.getSuggestedMinimumHeight()
        val drawableHeight = maxOf(
            track?.minimumHeight ?: 0,
            activeTrack?.minimumHeight ?: 0,
            thumb?.minimumHeight ?: 0
        )

        return maxOf(minimumHeight, drawableHeight)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = suggestedMinimumWidth + paddingLeft + paddingRight
        val desiredHeight = suggestedMinimumHeight + paddingTop + paddingBottom

        val measuredWidth = resolveSizeAndState(desiredWidth, widthMeasureSpec, 0)
        val measuredHeight = resolveSizeAndState(desiredHeight, heightMeasureSpec, 0)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    /**
     * Convert the value of one bound in `[min ; max]` to fit in `[0.0, 1.0]`.
     */
    private fun scaled(bound: Float): Float {
        val range = _maxValue - _minValue
        return if (range > 0f) (bound - _minValue) / range else 0f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val paddedWidth = w - paddingLeft - paddingRight

        activeBounds.apply {
            left = (scaled(_lowerValue) * paddedWidth).toInt()
            top = 0
            right = (scaled(_upperValue) * paddedWidth).toInt()
            bottom = h - paddingTop - paddingBottom
        }

        updateTrackAndThumbsPosition(w, h)
    }

    private fun updateTrackAndThumbsPosition(width: Int, height: Int) {
        val paddedWidth = width - paddingLeft - paddingRight
        val paddedHeight = height - paddingTop - paddingBottom

        val trackHeight = track?.intrinsicHeight ?: 0
        val activeHeight = activeTrack?.intrinsicHeight ?: 0
        val thumbHeight = thumb?.intrinsicHeight ?: 0

        // Find the tallest item, and offset the others so that they are centered vertically.
        val tallestHeight = maxOf(trackHeight, activeHeight, thumbHeight)
        val offsetHeight = (paddedHeight - tallestHeight) / 2

        val trackOffset = offsetHeight + (tallestHeight - trackHeight) / 2
        val activeOffset = offsetHeight + (tallestHeight - activeHeight) / 2
        val thumbOffset = offsetHeight + (tallestHeight - thumbHeight) / 2

        track?.setBounds(0, trackOffset, paddedWidth, trackOffset + trackHeight)
        activeTrack?.setBounds(0, activeOffset, paddedWidth, activeOffset + activeHeight)

        if (thumb != null) {
            setThumbPosition(width, lowerThumbBounds, scaled(_lowerValue), thumbOffset)
            setThumbPosition(width, upperThumbBounds, scaled(_upperValue), thumbOffset)
        }
    }

    private fun setThumbPosition(width: Int, thumbBounds: Rect, scale: Float, offset: Int) {
        check(thumb != null)
        val paddedWidth = width - paddingLeft - paddingRight
        val thumbWidth = thumb.intrinsicWidth
        val thumbHeight = thumb.intrinsicHeight

        val thumbPosition = (scale * paddedWidth).toInt()
        thumbBounds.apply {
            left = thumbPosition
            top = offset
            right = left + thumbWidth
            bottom = offset + thumbHeight
        }
    }

    override fun onDraw(canvas: Canvas) {
        drawBar(canvas)
        drawTickMarks(canvas)
        drawThumbs(canvas)
    }

    private fun drawBar(canvas: Canvas) {
        if (track != null || activeTrack != null) {
            canvas.withSave {
                translate(paddingLeft.toFloat(), paddingTop.toFloat())

                if (track != null) {
                    canvas.withSave {
                        clipRect(activeBounds, Region.Op.DIFFERENCE)
                        track.draw(this)
                    }
                }

                if (activeTrack != null) {
                    canvas.withSave {
                        clipRect(activeBounds)
                        activeTrack.draw(this)
                    }
                }
            }
        }
    }

    private fun drawThumbs(canvas: Canvas) {
        if (thumb != null) {
            val thumbOffset = thumb.intrinsicWidth / 2f
            canvas.withSave {
                translate(paddingLeft - thumbOffset, paddingTop.toFloat())
                thumb.bounds = lowerThumbBounds
                thumb.draw(canvas)

                thumb.bounds = upperThumbBounds
                thumb.draw(canvas)
            }
        }
    }

    private fun drawTickMarks(canvas: Canvas) {
        if (tickMark != null && step > STEP_CONTINUOUS) {
            val numberOfTicks = ((_maxValue - _minValue) / step).toInt()
            if (numberOfTicks > 1) {
                val w = tickMark.intrinsicWidth
                val h = tickMark.intrinsicHeight
                val halfW = if (w >= 0) w / 2 else 1
                val halfH = if (h >= 0) h / 2 else 1
                tickMark.setBounds(-halfW, -halfH, halfW, halfH)

                val spacing = (width - paddingLeft - paddingRight).toFloat() / numberOfTicks
                val medianHeight = (height - paddingTop - paddingBottom) / 2

                canvas.withSave {
                    translate(paddingLeft.toFloat(), (paddingTop + medianHeight).toFloat())

                    repeat(numberOfTicks + 1) {
                        tickMark.draw(this)
                        translate(spacing, 0f)
                    }
                }
            }
        }
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()

        val viewState = drawableState
        val trackChanged = track?.isStateful == true && track.setState(viewState)
        val activeTrackChanged = activeTrack?.isStateful == true && activeTrack.setState(viewState)
        val thumbChanged = thumb?.isStateful == true && thumb.setState(viewState)
        val tickMarkChanged = tickMark?.isStateful == true && tickMark.setState(viewState)

        if (trackChanged || activeTrackChanged || thumbChanged || tickMarkChanged) {
            invalidate()
        }
    }

    /**
     * If this returns true, we can't start dragging the Slider immediately
     * when we receive a [MotionEvent.ACTION_DOWN].
     * Instead, we must wait for a [MotionEvent.ACTION_MOVE].
     *
     * Copied from hidden method of [View] isInScrollingContainer.
     *
     * @return true if any of this View's parents is a scrolling View.
     */
    private fun isInScrollingContainer(): Boolean {
        var p = parent
        while (p is ViewGroup) {
            if (p.shouldDelayChildPressedState()) {
                return true
            }
            p = p.getParent()
        }
        return false
    }

    /**
     * Sets a listener to receive notifications of changes to the RangeSlider's bounds.
     * Also provides notifications of when the user starts and stops a touch gesture within the Slider.
     *
     * @param listener The slider notification listener.
     *
     * @see RangeSlider.OnRangeChangedListener
     */
    fun setOnRangeChangedListener(listener: OnRangeChangedListener?) {
        rangeListener = listener
    }

    /**
     * A callback that notifies clients when the range bounds have been changed
     * by the user through a touch gesture.
     */
    interface OnRangeChangedListener {
        fun onRangeChanged(slider: RangeSlider, lower: Float, upper: Float)
        fun onStartTrackingTouch(slider: RangeSlider)
        fun onStopTrackingTouch(slider: RangeSlider)
    }
}

/**
 * The default minimum value.
 */
private const val DEFAULT_MIN_VALUE = 0.0f

/**
 * The default maximum value.
 */
private const val DEFAULT_MAX_VALUE = 1.0f

/**
 * Special value for the step property of [RangeSlider] that causes it to run in continuous mode.
 */
private const val STEP_CONTINUOUS = 0.0f

/**
 * The default number of tick marks to display when no step is specified.
 */
private const val DEFAULT_TICK_MARKS = 10

/**
 * The maximum number of tick marks to display when the step is too small or invalid.
 */
private const val MAX_TICK_MARKS = 20