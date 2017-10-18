package fr.nihilus.music.utils

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog

/**
 * A Fragment that displays an AlertDialog that can be used to confirm user decisions.
 *
 * UI events related to the dialog such as button clicks or dialog dismiss are forwarded to the
 * caller's fragment [Fragment.onActivityResult] method:
 *
 * The supplied result code may be one of the following :
 * - [DialogInterface.BUTTON_POSITIVE] if the user selected the positive button,
 * - [DialogInterface.BUTTON_NEGATIVE] if the user selected the negative button,
 * - [DialogInterface.BUTTON_NEUTRAL] if the user selected selected the neutral button,
 * - [ConfirmDialogFragment.RESULT_DISMISSED] if the dialog is dismissed.
 *
 */
class ConfirmDialogFragment : DialogFragment(), DialogInterface.OnClickListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = checkNotNull(arguments) {
            "This fragment must be created by the newInstance method"
        }

        val builder = AlertDialog.Builder(context)
                .setTitle(args.getString(ARG_TITLE))

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

    override fun onDismiss(dialog: DialogInterface?) {
        targetFragment.onActivityResult(targetRequestCode, RESULT_DISMISSED, null)
        super.onDismiss(dialog)
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        targetFragment.onActivityResult(targetRequestCode, which, null)
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_POSITIVE = "positive_button"
        private const val ARG_NEGATIVE = "negative_button"
        private const val ARG_NEUTRAL = "neutral_button"

        /**
         * Result code indicating that the dialog has been dismissed by the user.
         */
        const val RESULT_DISMISSED = -42

        /**
         * Create a new instance of this DialogFragment.
         *
         * @param caller the Fragment that displayed this dialog,
         * to which the result should be forwarded
         * @param requestCode a number identifying the request that'll
         * be forwarded to [Fragment.onActivityResult] once interaction with the dialog is finished
         * @param title the title of the dialog to display
         * @param message an optional message to display in the dialog's body
         * @param positiveButton an optional resource id of the text
         * to display in the positive button. If absent, no positive button will be shown.
         * @param negativeButton an optional resource id of the text
         * to display in the negative button. If absent, no negative button will be shown.
         * @param neutralButton an optional resource id of the text
         * to display in the neutral button. If absent, no neutral button will be shown.
         */
        @JvmStatic fun newInstance(
                caller: Fragment,
                requestCode: Int,
                title: String,
                message: String? = null,
                @StringRes positiveButton: Int = 0,
                @StringRes negativeButton: Int = 0,
                @StringRes neutralButton: Int = 0
        ): ConfirmDialogFragment {
            val args = Bundle(5)
            args.putString(ARG_TITLE, title)
            args.putString(ARG_MESSAGE, message)
            args.putInt(ARG_POSITIVE, positiveButton)
            args.putInt(ARG_NEGATIVE, negativeButton)
            args.putInt(ARG_NEUTRAL, neutralButton)

            val fragment = ConfirmDialogFragment()
            fragment.arguments = args
            fragment.setTargetFragment(caller, requestCode)
            return fragment
        }
    }
}