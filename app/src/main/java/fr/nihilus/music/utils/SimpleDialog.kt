package fr.nihilus.music.utils

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog

private const val ARG_TITLE = "title"
private const val ARG_MESSAGE = "message"

class SimpleDialog: DialogFragment() {

    private val actions: Array<Action> = Array(2) { NoopAction }

    fun setPositiveAction(@StringRes label: Int, action: DialogInterface.OnClickListener): SimpleDialog {
        actions[0] = Action(label, action)
        return this
    }

    fun setNegativeAction(@StringRes label: Int, action: DialogInterface.OnClickListener): SimpleDialog {
        actions[1] = Action(label, action)
        return this
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = arguments ?: throw IllegalArgumentException()
        return AlertDialog.Builder(context)
                .setTitle(args.getInt(ARG_TITLE))
                .setMessage(args.getInt(ARG_MESSAGE))
                .setPositiveButton(actions[0].label, actions[0].action)
                .setNegativeButton(actions[1].label, actions[1].action)
                .create()
    }

    companion object {
        fun newInstance(@StringRes title: Int, @StringRes message: Int = 0): SimpleDialog {
            val args = Bundle(2)
            args.putInt(ARG_TITLE, title)
            args.putInt(ARG_MESSAGE, message)
            val fragment = SimpleDialog()
            fragment.arguments = args
            return fragment
        }
    }

    open class Action(@StringRes val label: Int, val action: DialogInterface.OnClickListener)
    private object NoopAction: Action(0, DialogInterface.OnClickListener { _, _ -> })
}