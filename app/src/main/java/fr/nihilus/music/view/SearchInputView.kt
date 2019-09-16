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
import android.text.Editable
import android.util.AttributeSet
import android.widget.ListAdapter
import androidx.annotation.StringRes
import androidx.appcompat.view.CollapsibleActionView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.appcompat.widget.ListPopupWindow

class SearchInputView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayoutCompat(context, attrs), CollapsibleActionView {

    private val suggestionWindow = ListPopupWindow(context)
    private val textChangedActions = mutableSetOf<(Editable?) -> Unit>()

    init {
        suggestionWindow.anchorView = this
        suggestionWindow.isModal = true
        suggestionWindow.promptPosition = ListPopupWindow.POSITION_PROMPT_ABOVE
    }

    override fun onActionViewExpanded() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onActionViewCollapsed() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun doAfterTextChanged(action: (Editable?) -> Unit) {
        textChangedActions += action
    }

    fun setHint(@StringRes resId: Int) {
        TODO("Set the EditText hint.")
    }

    fun setAdapter(adapter: ListAdapter) {
        suggestionWindow.setAdapter(adapter)
        // TODO Show popup on receiving search results, or hide if there are none.
    }

    fun setOnSuggestionSelected(listener: ((position: Int) -> Unit)?) {
        if (listener != null) {
            suggestionWindow.setOnItemClickListener { _, _, position, _ ->
                listener(position)
            }
        } else {
            suggestionWindow.setOnItemClickListener(null)
        }
    }
}