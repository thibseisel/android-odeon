/*
 * Copyright 2020 Thibault Seisel
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

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.nihilus.music.core.R
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

/**
 * Main interface for reading and writing to the device's file system.
 * This should be preferred over manipulating [File] instances.
 */
interface FileSystem {

    @Throws(IOException::class)
    fun writeBitmapToInternalStorage(filepath: String, bitmap: Bitmap): Uri?

    /**
     * Share the file at [filePath], returning an uri that can be resolved from other applications.
     * The folder containing tha file should be configured as sharable.
     *
     * @param filePath Path to the file that should be shared.
     * @return An uri pointing to the specified file, or `null` if such file does not exists.
     */
    fun makeSharedContentUri(filePath: String): Uri?

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
internal class AndroidFileSystem @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("internal") private val internalRootDir: File
) : FileSystem {

    override fun writeBitmapToInternalStorage(filepath: String, bitmap: Bitmap): Uri? {
        val lastSeparatorPosition = filepath.lastIndexOf('/')

        val dirPath = filepath.substring(0, lastSeparatorPosition)
        val fileDir = File(internalRootDir, dirPath)
        if (!fileDir.exists()) {
            fileDir.mkdirs()
        }

        val filename = filepath.substring(lastSeparatorPosition + 1)
        val bitmapFile = File(fileDir, filename)
        val successfullyWritten = bitmapFile.outputStream().use { outputFile ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputFile)
        }

        return if (successfullyWritten) FileProvider.getUriForFile(
            context,
            context.getString(R.string.core_provider_authority),
            bitmapFile
        ) else null
    }

    override fun makeSharedContentUri(filePath: String): Uri? {
        val file = File(filePath)
        return if (file.exists()) {
            FileProvider.getUriForFile(context, context.getString(R.string.core_provider_authority), file)
        } else null
    }

    override fun deleteFile(filepath: String): Boolean {
        val file = File(filepath)
        return file.isFile && file.delete()
    }
}