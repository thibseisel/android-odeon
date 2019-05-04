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

package fr.nihilus.music.media.os

import fr.nihilus.music.media.fail

/**
 * A simulated file system used for testing purposes.
 * No actual files are stored.
 */
class SimulatedFileSystem(
    vararg filenames: String
) : FileSystem {
    private val storedFiles = filenames.toMutableSet()

    override fun deleteFile(filepath: String): Boolean = storedFiles.remove(filepath)
    fun fileExists(filepath: String): Boolean = storedFiles.contains(filepath)
}

/**
 * A stub file system that always fails.
 * Using this as a dependency ensures that the file system is not accessed,
 * otherwise the test will fail.
 */
object StubFileSystem : FileSystem {
    override fun deleteFile(filepath: String): Boolean {
        fail("Expected not to interact with file system.")
    }
}