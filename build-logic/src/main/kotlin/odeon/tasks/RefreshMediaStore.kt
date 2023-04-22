/*
 * Copyright 2022 Thibault Seisel
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

package odeon.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

/**
 * Scan for music files stored on the connected Android device or emulator, updating which files
 * are listed by `MediaStore.Audio`.…
 * You may need to run this task after uploading audio files to an Android emulator,
 * as their media scanner is not run automatically for some reasons.
 */
internal abstract class RefreshMediaStore : DefaultTask() {

    /**
     * The path to the Android Debug Bridge (ADB) executable from the Android SDK.
     * You may use `androidComponents.sdkComponents.adb` for convenience.
     */
    @get:InputFile
    abstract val adbPath: RegularFileProperty

    @TaskAction
    private fun execute() {
        val adbFile = adbPath.asFile.get()
        require(adbFile.canExecute()) {
            "adb executable is missing or requires exec file permissions"
        }
        val command = ProcessBuilder(
            adbFile.absolutePath,
            "shell",
            """
            find /storage/emulated/0/Music/ -type f | while read f; do \
            am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE \
            -d "file://${'$'}{f}"; done
            """.trimIndent()
        ).start()

        if (logger.isInfoEnabled) {
            command.inputStream.bufferedReader().useLines {
                it.forEach(logger::info)
            }
        }

        val exitCode = command.waitFor()
        if (exitCode != 0) {
            val errorOutput = command.errorStream.bufferedReader().use { it.readText() }
            error(errorOutput)
        }
    }
}
