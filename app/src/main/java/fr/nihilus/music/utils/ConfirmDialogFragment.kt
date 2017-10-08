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
 * caller's fragment [Fragment.onActivityResult] method.
 */
class ConfirmDialogFragment : DialogFragment(), DialogInterface.OnClickListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
                .setTitle(arguments.getInt(ARG_TITLE))

        val message = arguments.getInt(ARG_MESSAGE)
        if (message != 0) {
            builder.setMessage(message)
        }

        val positive = arguments.getInt(ARG_POSITIVE)
        if (positive != 0) {
            builder.setPositiveButton(positive, this)
        }

        val negative = arguments.getInt(ARG_NEGATIVE)
        if (negative != 0) {
            builder.setNegativeButton(negative, this)
        }

        val neutral = arguments.getInt(ARG_NEUTRAL)
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

        const val RESULT_DISMISSED = -42


        fun newInstance(
                caller: Fragment,
                requestCode: Int,
                @StringRes title: Int,
                @StringRes message: Int = 0,
                @StringRes positiveButton: Int = 0,
                @StringRes negativeButton: Int = 0,
                @StringRes neutralButton: Int = 0
        ): ConfirmDialogFragment {
            val args = Bundle(5)
            args.putInt(ARG_TITLE, title)
            args.putInt(ARG_MESSAGE, message)
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