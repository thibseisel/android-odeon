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

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import fr.nihilus.music.common.test.stub
import fr.nihilus.music.media.os.BasicFileSystem.makeSharedContentUri

/**
 * A simulated file system used for testing purposes.
 * No actual files are stored.
 */
internal class SimulatedFileSystem(
    vararg filenames: String
) : FileSystem {
    private val storedFiles = filenames.toMutableSet()

    override fun writeBitmapToInternalStorage(filepath: String, bitmap: Bitmap): Uri? = stub()

    override fun makeSharedContentUri(filePath: String): Uri? {
        val albumArtSpecificPart = filePath.substringAfter("albumthumbs/")
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority("fr.nihilus.music.media.test.provider")
            .appendPath("albumthumbs")
            .appendPath(albumArtSpecificPart)
            .build()
    }

    override fun deleteFile(filepath: String): Boolean = storedFiles.remove(filepath)

    fun fileExists(filepath: String): Boolean = storedFiles.contains(filepath)
}

/**
 * A fake file system whose only valid operation is [makeSharedContentUri].
 * other functions are stubbed and will fail.
 */
object BasicFileSystem : FileSystem {
    override fun writeBitmapToInternalStorage(filepath: String, bitmap: Bitmap): Uri? = stub()
    override fun deleteFile(filepath: String): Boolean = stub()

    /**
     * For simplicity, this only convert the passed string to an Uri,
     * without making it sharable.
     */
    override fun makeSharedContentUri(filePath: String): Uri? {
        val albumArtSpecificPart = filePath.substringAfter("albumthumbs/")
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority("fr.nihilus.music.media.test.provider")
            .appendPath("albumthumbs")
            .appendPath(albumArtSpecificPart)
            .build()
    }
}