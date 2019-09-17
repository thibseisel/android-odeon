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
import android.text.Editable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ListAdapter
import androidx.annotation.StringRes
import androidx.appcompat.view.CollapsibleActionView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.widget.doAfterTextChanged
import fr.nihilus.music.R
import kotlinx.android.synthetic.main.view_search_input.view.*

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

    private var adapter: ListAdapter? = null

    init {
        // Inflate the content of this view from a layout file.
        val root = View.inflate(context, R.layout.view_search_input, this)
        textInput = root.findViewById(R.id.search_input)
        textInput.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN

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