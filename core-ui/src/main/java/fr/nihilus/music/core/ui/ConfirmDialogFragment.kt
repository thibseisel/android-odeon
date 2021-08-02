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

package fr.nihilus.music.core.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.nihilus.music.core.ui.ConfirmDialogFragment.Companion.open
import fr.nihilus.music.core.ui.ConfirmDialogFragment.Companion.registerForResult

/**
 * A Fragment that displays an AlertDialog that can be used to confirm simple user decisions.
 *
 * Instances should not be created directly ; instead a caller [Fragment] should call [open]
 * to specify the content of the dialog and listen for the clicked action button with [registerForResult].
 */
class ConfirmDialogFragment internal constructor() : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val arguments = requireArguments()
        val requestKey = checkNotNull(arguments.getString(ARG_REQUEST_KEY))
        
        val dispatchDialogResult = DialogInterface.OnClickListener { _, which ->
            val selectedButton = when (which) {
                DialogInterface.BUTTON_POSITIVE -> ActionButton.POSITIVE
                DialogInterface.BUTTON_NEGATIVE -> ActionButton.NEGATIVE
                DialogInterface.BUTTON_NEUTRAL -> ActionButton.NEUTRAL
                else -> error("Unexpected AlertDialog button: $which")
            }
            setFragmentResult(requestKey, Bundle().apply {
                putInt(KEY_RESULT_BUTTON, selectedButton.ordinal)
            })
        }

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(arguments.getString(ARG_TITLE))
            .setMessage(arguments.getString(ARG_MESSAGE))

        val positive = arguments.getInt(ARG_POSITIVE)
        if (positive != 0) {
            builder.setPositiveButton(positive, dispatchDialogResult)
        }

        val negative = arguments.getInt(ARG_NEGATIVE)
        if (negative != 0) {
            builder.setNegativeButton(negative, dispatchDialogResult)
        }

        val neutral = arguments.getInt(ARG_NEUTRAL)
        if (neutral != 0) {
            builder.setNeutralButton(neutral, dispatchDialogResult)
        }

        return builder.create()
    }

    companion object {
        private const val ARG_REQUEST_KEY = "fr.nihilus.music.ui.DIALOG_REQUEST_KEY"
        private const val ARG_TITLE = "fr.nihilus.music.ui.DIALOG_TITLE"
        private const val ARG_MESSAGE = "fr.nihilus.music.ui.DIALOG_MESSAGE"
        private const val ARG_POSITIVE = "fr.nihilus.music.ui.DIALOG_POSITIVE_BUTTON"
        private const val ARG_NEGATIVE = "fr.nihilus.music.ui.DIALOG_NEGATIVE_BUTTON"
        private const val ARG_NEUTRAL = "fr.nihilus.music.ui.DIALOG_NEUTRAL_BUTTON"

        private const val KEY_RESULT_BUTTON = "fr.nihilus.music.ui.DIALOG_BUTTON"

        /**
         * Prepare and display a confirm dialog above content.
         *
         * @param caller Caller activity.
         * @param requestKey String identifying the caller to receive the response.
         * @param title Title of the displayed dialog.
         * @param message Optional message to display as the dialog's body.
         * @param positiveButton Optional string resource id used as the positive button's label.
         * @param negativeButton Optional string resource id used as the negative button's label.
         * @param neutralButton Optional string resource id used as the neutral button's label.
         */
        fun open(
            caller: AppCompatActivity,
            requestKey: String,
            title: String? = null,
            message: String? = null,
            @StringRes positiveButton: Int = 0,
            @StringRes negativeButton: Int = 0,
            @StringRes neutralButton: Int = 0
        ) = open(
            caller.supportFragmentManager,
            requestKey,
            title,
            message,
            positiveButton,
            negativeButton,
            neutralButton
        )

        /**
         * Prepare and display a confirm dialog above content.
         *
         * @param caller Caller fragment.
         * @param requestKey String identifying the caller fragment to receive the response.
         * @param title Title of the displayed dialog.
         * @param message Optional message to display as the dialog's body.
         * @param positiveButton Optional string resource id used as the positive button's label.
         * @param negativeButton Optional string resource id used as the negative button's label.
         * @param neutralButton Optional string resource id used as the neutral button's label.
         */
        fun open(
            caller: Fragment,
            requestKey: String,
            title: String? = null,
            message: String? = null,
            @StringRes positiveButton: Int = 0,
            @StringRes negativeButton: Int = 0,
            @StringRes neutralButton: Int = 0
        ) = open(
            caller.childFragmentManager,
            requestKey,
            title,
            message,
            positiveButton,
            negativeButton,
            neutralButton
        )

        private fun open(
            callerFragmentManager: FragmentManager,
            requestKey: String,
            title: String? = null,
            message: String? = null,
            @StringRes positiveButton: Int = 0,
            @StringRes negativeButton: Int = 0,
            @StringRes neutralButton: Int = 0
        ) {
            val dialog = ConfirmDialogFragment().apply {
                arguments = Bundle(5).apply {
                    putString(ARG_REQUEST_KEY, requestKey)
                    putString(ARG_TITLE, title)
                    putString(ARG_MESSAGE, message)
                    putInt(ARG_POSITIVE, positiveButton)
                    putInt(ARG_NEGATIVE, negativeButton)
                    putInt(ARG_NEUTRAL, neutralButton)
                }
            }

            dialog.show(callerFragmentManager, null)
        }

        /**
         * Registers a [FragmentResultListener] to receive the clicked dialog button as the result.
         * @param caller Fragment that should receive the result.
         * @param requestKey The same request key used to [open] the dialog.
         * @param listener A function called with the dialog result when available.
         *
         * @see setFragmentResultListener
         */
        fun registerForResult(
            caller: Fragment,
            requestKey: String,
            listener: (button: ActionButton) -> Unit
        ) {
            caller.childFragmentManager.setFragmentResultListener(requestKey, caller) { key, result ->
                if (requestKey == key) {
                    val resultCode = result.getInt(KEY_RESULT_BUTTON)
                    val clickedButton = ActionButton.values()[resultCode]
                    listener(clickedButton)
                }
            }
        }
    }

    /**
     * Dialog action button that was clicked.
     */
    enum class ActionButton {
        POSITIVE,
        NEGATIVE,
        NEUTRAL
    }
}
