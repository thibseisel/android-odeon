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

package fr.nihilus.music.core.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import fr.nihilus.music.core.os.PlaylistIconDir
import java.io.File

private const val PLAYLIST_ICONS_URI_PATH = "icons"
private const val FALLBACK_MIME_TYPE = "application/octet-stream"

/**
 * Provides generated playlist icons to external applications that connects to Odeon media browser.
 */
internal class IconProvider : ContentProvider() {
    private val defaultColumns = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val file = getFileForUri(uri)
        val columns = projection ?: defaultColumns
        val cursor = MatrixCursor(columns.copyOf())
        cursor.newRow()
            .add(OpenableColumns.DISPLAY_NAME, file.name)
            .add(OpenableColumns.SIZE, file.length())
        return cursor
    }

    override fun getType(uri: Uri): String {
        val file = getFileForUri(uri)
        return file.extension.takeUnless { it.isEmpty() }
            ?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
            ?: FALLBACK_MIME_TYPE
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        if (mode != "r") throw SecurityException("File is read-only")
        val file = getFileForUri(uri)
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException("No external inserts")
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        throw UnsupportedOperationException("No external updates")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        throw UnsupportedOperationException("No external deletes")
    }

    override fun shutdown() {
        // Nothing to do.
        // The default implementation prints inappropriate warnings to the console.
    }

    private fun getFileForUri(uri: Uri): File {
        val path = requireNotNull(uri.encodedPath)
        val appContext = checkNotNull(context).applicationContext

        val entryPoint = EntryPointAccessors.fromApplication(appContext, ProviderEntryPoint::class.java)
        val filename = path.substringAfter("$PLAYLIST_ICONS_URI_PATH/")
        return File(entryPoint.iconDir(), filename)
    }

    /**
     * Configure an entry point to inject dependencies into the content provider.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ProviderEntryPoint {
        @PlaylistIconDir fun iconDir(): File
    }
}