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

package fr.nihilus.music.ui

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment

/**
 * A Fragment that displays an AlertDialog that can be used to confirm user decisions.
 *
 * UI events related to the dialog such as button clicks or dialog dismiss are forwarded to the
 * caller's fragment [Fragment.onActivityResult] method:
 *
 * The supplied result code may be one of the following :
 * - [DialogInterface.BUTTON_POSITIVE] if the user selected the positive button,
 * - [DialogInterface.BUTTON_NEGATIVE] if the user selected the negative button,
 * - [DialogInterface.BUTTON_NEUTRAL] if the user selected the neutral button,
 * - [Activity.RESULT_CANCELED] if the dialog is canceled as a result of pressing back button
 * or taping out of the dialog frame.
 */
class ConfirmDialogFragment : DialogFragment(), DialogInterface.OnClickListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = checkNotNull(arguments) {
            "Instances of ConfirmDialogFragment should be created using the newInstance method."
        }

        val builder = AlertDialog.Builder(context!!).setTitle(args.getString(ARG_TITLE))

        val message = args.getString(ARG_MESSAGE)
        builder.setMessage(message)

        val positive = args.getInt(ARG_POSITIVE)
        if (positive != 0) {
            builder.setPositiveButton(positive, this)
        }

        val negative = args.getInt(ARG_NEGATIVE)
        if (negative != 0) {
            builder.setNegativeButton(negative, this)
        }

        val neutral = args.getInt(ARG_NEUTRAL)
        if (neutral != 0) {
            builder.setNeutralButton(neutral, this)
        }

        return builder.create()
    }

    override fun onCancel(dialog: DialogInterface?) {
        targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_CANCELED, null)
        super.onCancel(dialog)
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        targetFragment?.onActivityResult(targetRequestCode, which, null)
    }

    companion object Factory {
        private const val ARG_TITLE = "dialog_title"
        private const val ARG_MESSAGE = "dialog_message"
        private const val ARG_POSITIVE = "dialog_positive_button"
        private const val ARG_NEGATIVE = "dialog_negative_button"
        private const val ARG_NEUTRAL = "dialog_neutral_button"

        /**
         * Create a new instance of this DialogFragment.
         *
         * @param caller the Fragment that displays this dialog
         * to which the result should be forwarded.
         * @param requestCode a number identifying the request that'll
         * be forwarded to [Fragment.onActivityResult] once interaction with the dialog is finished.
         * @param title the title of the dialog to display.
         * @param message an optional message to display as the dialog's body.
         * @param positiveButton an optional resource id of the text.
         * to display in the positive button. If 0, no positive button will be shown.
         * @param negativeButton an optional resource id of the text
         * to display in the negative button. If 0, no negative button will be shown.
         * @param neutralButton an optional resource id of the text
         * to display in the neutral button. If 0, no neutral button will be shown.
         */
        @JvmStatic fun newInstance(
            caller: Fragment?,
            requestCode: Int,
            title: String? = null,
            message: String? = null,
            @StringRes positiveButton: Int = 0,
            @StringRes negativeButton: Int = 0,
            @StringRes neutralButton: Int = 0

        ) = ConfirmDialogFragment().apply {
            setTargetFragment(caller, requestCode)
            arguments = Bundle(5).apply {
                putString(ARG_TITLE, title)
                putString(ARG_MESSAGE, message)
                putInt(ARG_POSITIVE, positiveButton)
                putInt(ARG_NEGATIVE, negativeButton)
                putInt(ARG_NEUTRAL, neutralButton)
            }
        }
    }
}