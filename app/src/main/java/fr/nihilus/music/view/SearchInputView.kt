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
import android.database.DataSetObserver
import android.graphics.Rect
import android.os.Build
import android.text.Editable
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import androidx.appcompat.view.CollapsibleActionView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.widget.doAfterTextChanged
import fr.nihilus.music.R

/**
 * A combo View that displays a text field associated with a popup window.
 * The popup is displayed while typing and its content is backed by an [adapter][setAdapter].
 */
class SearchInputView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayoutCompat(context, attrs), CollapsibleActionView {

    private val popupVisibilityObserver = SuggestionPopupVisibilityObserver()

    private val suggestionWindow: ListPopupWindow
    private val textInput: EditText
    private val closeButton: ImageView

    private var adapter: ListAdapter? = null

    init {
        // Inflate the content of this view from a layout file.
        val root = View.inflate(context, R.layout.view_search_input, this)
        textInput = root.findViewById(R.id.search_input)
        textInput.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN

        closeButton = root.findViewById(R.id.close_button)

        suggestionWindow = ListPopupWindow(context).apply {
            promptPosition = ListPopupWindow.POSITION_PROMPT_ABOVE
            anchorView = root
            isModal = true
        }
    }

    override fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean {
        if (!isFocusable) return super.requestFocus()
        return textInput.requestFocus(direction, previouslyFocusedRect)
    }

    override fun clearFocus() {
        super.clearFocus()
        textInput.clearFocus()
    }

    override fun onActionViewCollapsed() {
        // Clear text and focus.
        textInput.text = null
        clearFocus()
    }

    override fun onActionViewExpanded() {
        // Reset input text.
        textInput.text = null
    }

    /**
     * Register an action to be performed some time after the typed text has been modified.
     *
     * @param action A callback function called after text has been changed.
     */
    fun doAfterTextChanged(action: (text: Editable?) -> Unit) {
        textInput.doAfterTextChanged(action)
    }

    /**
     * Sets the displayed hint for the text input.
     *
     * @param resId The identifier of a string resource to be used as the hint.
     */
    fun setQueryHint(@StringRes resId: Int) {
        textInput.setHint(resId)
    }

    /**
     * Sets the adapter that provides the data and the views to represent the data
     * in the suggestions popup window.
     *
     * @param adapter The adapter to use to create the suggestion popup content.
     */
    fun setAdapter(adapter: ListAdapter?) {
        this.adapter?.unregisterDataSetObserver(popupVisibilityObserver)
        this.adapter = adapter
        suggestionWindow.setAdapter(adapter)
        adapter?.registerDataSetObserver(popupVisibilityObserver)
    }

    /**
     * Register a callback function to be called when a suggestion is selected
     * from the suggestion popup window.
     *
     * @param listener The callback function, called with the position of the selected suggestion.
     */
    fun setOnSuggestionSelected(listener: ((position: Int) -> Unit)?) {
        if (listener != null) {
            suggestionWindow.setOnItemClickListener { _, _, position, _ ->
                listener(position)
            }
        } else {
            suggestionWindow.setOnItemClickListener(null)
        }
    }

    class SearchTextView
    @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        @AttrRes defStyleAttr: Int = 0
    ) : AppCompatTextView(context, attrs, defStyleAttr) {

        private val popup: ListPopupWindow
        private val popupContext: Context

        private val forwardingClickListener = ForwardingClickListener()

        private var dropDownAnchorId: Int
        private lateinit var dropDOwnAnchorView: View

        /**
         * When `true`, an update of the underlying adapter will update the content of the list popup.
         * Set to `false` when the list is hidden to prevent asynchronous to the popup list.
         */
        private var popupCanBeUpdated = false

        private var adapter: ListAdapter? = null
            set(value) {
                field = value
                popup.setAdapter(value)
            }

        /**
         * The current width for the auto-complete drop down list.
         * This can be a fixed width, or [ViewGroup.LayoutParams.MATCH_PARENT] to fill the screen,
         * or [ViewGroup.LayoutParams.WRAP_CONTENT] to fit the width of its anchor view.
         *
         * @attr ref R.styleable.SearchTextView_dropDownWidth
         */
        var dropDownWidth: Int
            get() = popup.width
            set(value) { popup.width = value }

        /**
         * The current height for the auto-complete drop down list.
         * This can be a fixed height, or [ViewGroup.LayoutParams.MATCH_PARENT] to fill the screen,
         * or [ViewGroup.LayoutParams.WRAP_CONTENT] to fit the height of the dropdown's content.
         *
         * @attr ref R.styleable.SearchTextView_dropDownHeight
         */
        var dropDownHeight: Int
            get() = popup.height
            set(value) { popup.height = value }

        /**
         * The id for the view that the auto-complete dropdown list is anchored to.
         *
         * @attr ref R.styleable.SearchTextView_dropDownAnchor
         */
        var dropDownAnchor: Int
            get() = dropDownAnchorId
            set(value) {
                dropDownAnchorId = value
                popup.anchorView = null
            }

        /**
         * Whether the dropdown is dismissed when a suggestion is clicked.
         * This is `true` by default.
         */
        var isDropDownDismissedOnCompletion: Boolean = true

        /**
         * The minimum number of characters the user must type before the dropdown list is shown.
         *
         * @attr ref R.styleable.SearchTextView_threshold
         */
        var threshold: Int = 0
            set(value) {
                field = value.coerceAtLeast(0)
            }

        init {
            val a = context.obtainStyledAttributes(attrs, R.styleable.SearchTextView)
            try {
                // Try to apply the specified custom theme for the suggestion popup window, if any.
                val popupThemeResId = a.getResourceId(R.styleable.SearchTextView_popupTheme, 0)
                if (popupThemeResId != 0) {
                    popupContext = ContextThemeWrapper(context, popupThemeResId)
                } else {
                    popupContext = context
                }

                val popupWidth = a.getLayoutDimension(R.styleable.SearchTextView_dropDownWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
                val popupHeight = a.getLayoutDimension(R.styleable.SearchTextView_dropDownHeight, ViewGroup.LayoutParams.WRAP_CONTENT)

                // Get the anchor view id now, but that view won't be ready yet,
                // so wait for the view to be inflated before getting its reference.
                dropDownAnchorId = a.getResourceId(R.styleable.SearchTextView_dropDownAnchor, View.NO_ID)

                popup = ListPopupWindow(popupContext, attrs, defStyleAttr).apply {
                    width = popupWidth
                    height = popupHeight
                    softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    promptPosition = ListPopupWindow.POSITION_PROMPT_BELOW
                }

                threshold = a.getInt(R.styleable.SearchTextView_threshold, 2)

            } finally {
                a.recycle()
            }

            // Always turn on the auto-complete input type flag,
            // since it makes no sense to use this widget without it.
            if (inputType and EditorInfo.TYPE_MASK_CLASS == EditorInfo.TYPE_CLASS_TEXT) {
                setRawInputType(inputType or EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE)
            }

            isFocusable = true

            setOnClickListener(forwardingClickListener)
        }

        fun setOnItemClickListener(listener: AdapterView.OnItemClickListener) {
            popup.setOnItemClickListener(listener)
        }

        private inner class ForwardingClickListener : OnClickListener {

            val wrapped: OnClickListener? = null

            override fun onClick(v: View?) {
                internalOnClick()
                wrapped?.onClick(v)
            }
        }

        private fun internalOnClick() {
            // If the dropdown is showing,
            // bring the keyboard to the front when the user touches the text field.
            if (isPopupShowing()) {
                ensureImeVisible(true)
            }
        }

        override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
            super.onWindowFocusChanged(hasWindowFocus)
            if (!hasWindowFocus) {
                dismissDropDown()
            }
        }

        override fun onFocusChanged(
            focused: Boolean,
            direction: Int,
            previouslyFocusedRect: Rect?
        ) {
            super.onFocusChanged(focused, direction, previouslyFocusedRect)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isTemporarilyDetached) {
                return
            }

            if (!focused) {
                dismissDropDown()
            }
        }

        override fun onDetachedFromWindow() {
            dismissDropDown()
            super.onDetachedFromWindow()
        }

        fun dismissDropDown() {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.displayCompletions(this, null)
            popup.dismiss()
            popupCanBeUpdated = false
        }

        override fun setFrame(l: Int, t: Int, r: Int, b: Int): Boolean {
            val result = super.setFrame(l, t, r, b)

            if (isPopupShowing()) {
                showDropDown()
            }

            return result
        }

        override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
            if (keyCode == KeyEvent.KEYCODE_BACK && isPopupShowing()) {
                // Special case for the back key,
                // we do not even try to send it to the dropdown list, but instead consume it.
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    keyDispatcherState?.startTracking(event, this)
                    return true
                } else if (event.action == KeyEvent.ACTION_UP) {
                    keyDispatcherState?.handleUpEvent(event)
                    if (event.isTracking && !event.isCanceled) {
                        dismissDropDown()
                        return true
                    }
                }
            }
            return super.onKeyPreIme(keyCode, event)
        }

        fun ensureImeVisible(visible: Boolean) {
            popup.inputMethodMode = if (visible) ListPopupWindow.INPUT_METHOD_NEEDED else ListPopupWindow.INPUT_METHOD_NOT_NEEDED
        }

        private fun showDropDown() {
            if (popup.anchorView == null) {
                if (dropDownAnchorId != View.NO_ID) {
                    popup.anchorView = rootView.findViewById(dropDownAnchorId)
                } else {
                    popup.anchorView = this
                }
            }

            if (!isPopupShowing()) {
                // Make sure the list does not obscure the IME when shown for the first time.
                popup.inputMethodMode = ListPopupWindow.INPUT_METHOD_NEEDED
            }

            popup.show()
            popup.listView!!.overScrollMode = View.OVER_SCROLL_ALWAYS
        }

        private fun isPopupShowing(): Boolean = popup.isShowing
    }

    private inner class SuggestionPopupVisibilityObserver : DataSetObserver() {

        override fun onChanged() {
            if (adapter?.isEmpty == true) {
                suggestionWindow.dismiss()
            } else {
                suggestionWindow.show()
            }
        }
    }
}