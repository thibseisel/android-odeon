@file:JvmName("MediaSessionCommand")
package fr.nihilus.music.command

import android.os.Bundle
import android.os.ResultReceiver

interface MediaSessionCommand {
    fun handle(params: Bundle?, cb: ResultReceiver?)

    companion object {
        const val CODE_UNKNOWN_COMMAND = -99
    }
}