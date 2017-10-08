package fr.nihilus.music.command

import android.os.Bundle
import android.os.ResultReceiver

/**
 * A custom command handler for media sessions.
 *
 * Each command is identified by a unique name, for example the implementation class name
 * prefixed by its package.
 */
interface MediaSessionCommand {

    /**
     * Handle the custom command.
     *
     * Commands may have parameters. If they do, they must check themselves the presence
     * and the validity of those parameters.
     * Also, if an error occurred while handling the command, this handler must notify the client
     * of an error by passing a specific error code to [cb].
     */
    fun handle(params: Bundle?, cb: ResultReceiver?)

    companion object {
        /**
         * Code supplied by [android.support.v4.media.session.MediaControllerCompat.sendCommand]
         */
        const val CODE_SUCCESS = 0
        const val CODE_UNEXPECTED_ERROR = -1
        const val CODE_UNKNOWN_COMMAND = -99
    }
}