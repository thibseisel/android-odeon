/*
 * Copyright 2018 Thibault Seisel
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
     * and validity of those parameters, throwing an [IllegalArgumentException]
     * if a parameter is missing or incorrect.
     *
     * Also, if an error occurred while handling the command, this handler must notify clients
     * of an error by passing a specific error code to [the result receiver][cb].
     *
     * @param params Parameters that may be needed to execute the command.
     */
    fun handle(params: Bundle?, cb: ResultReceiver?)

    /**
     * Holds result codes passed to [ResultReceiver.onReceiveResult]
     * after a command has been executed, denoting success or describing an error.
     */
    companion object ResultCode {

        /**
         * Indicates that the command successfully completed.
         * Every implementation of MediaSessionCommand should send this code upon success.
         */
        const val CODE_SUCCESS = 0

        /**
         * Indicates that a command can't be handled because it is not supported.
         * This code should not be used by `MediaSessionCommand` implementations.
         */
        const val CODE_UNKNOWN_COMMAND = -99
    }
}