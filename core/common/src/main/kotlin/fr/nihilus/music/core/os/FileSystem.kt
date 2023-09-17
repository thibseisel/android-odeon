/*
 * Copyright 2021 Thibault Seisel
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

package fr.nihilus.music.core.os

import dagger.Reusable
import java.io.File
import javax.inject.Inject

/**
 * Main interface for reading and writing to the device's file system.
 * This should be preferred over manipulating [File] instances.
 */
interface FileSystem {

    /**
     * Deletes a file located at [filepath] from the device's storage,
     * only if it exists and it is not a directory.
     *
     * @param filepath The absolute path to the file to be deleted.
     * @return `true` if the file has been successfully deleted, `false` otherwise.
     */
    fun deleteFile(filepath: String): Boolean
}

/**
 * Implementation of a real file system.
 * Operations are performed on real files stored on the device.
 */
@Reusable
internal class AndroidFileSystem @Inject constructor() : FileSystem {

    override fun deleteFile(filepath: String): Boolean {
        val file = File(filepath)
        return file.isFile && file.delete()
    }
}