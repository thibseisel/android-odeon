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

package fr.nihilus.music.media.provider

import android.app.PendingIntent.*
import android.content.*
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.BaseColumns
import androidx.core.os.bundleOf

/**
 * Test implementation of Android media provider.
 * This is only focused on providing a behavior to read and write audio media information.
 *
 * Some implementation details are extracted from the code source of
 * [MediaProvider](https://cs.android.com/android/platform/superproject/+/master:packages/providers/MediaProvider/src/com/android/providers/media/MediaProvider.java)
 * and [MediaStore](https://cs.android.com/android/platform/superproject/+/master:packages/providers/MediaProvider/apex/framework/java/android/provider/MediaStore.java).
 */
internal class FakeAudioMediaProvider : ContentProvider() {
    private companion object {
        const val ROOT = 0
        const val AUDIO_MEDIA = 100
        const val AUDIO_MEDIA_ID = 101
        const val AUDIO_ALBUMS = 200
        const val AUDIO_ALBUMS_ID = 201
        const val AUDIO_ARTISTS = 300
        const val AUDIO_ARTISTS_ID = 301

        private const val EXTRA_RESULT = "result"
        private const val CREATE_DELETE_REQUEST_CALL = "create_delete_request"
    }

    private lateinit var database: AudioMediaDatabase
    private lateinit var authority: String
    private lateinit var uriMatcher: UriMatcher

    override fun onCreate(): Boolean {
        database = AudioMediaDatabase(context!!)
        return true
    }

    override fun attachInfo(context: Context, info: ProviderInfo) {
        super.attachInfo(context, info)
        authority = info.authority

        uriMatcher = UriMatcher(ROOT).apply {
            addURI(authority, "*/audio/media", AUDIO_MEDIA)
            addURI(authority, "*/audio/media/#", AUDIO_MEDIA_ID)
            addURI(authority, "*/audio/albums", AUDIO_ALBUMS)
            addURI(authority, "*/audio/albums/#", AUDIO_ALBUMS_ID)
            addURI(authority, "*/audio/artists", AUDIO_ARTISTS)
            addURI(authority, "*/audio/artists/#", AUDIO_ARTISTS_ID)
        }
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && method == CREATE_DELETE_REQUEST_CALL) {
            val deleteMediaPendingIntent = getActivity(
                context!!,
                0,
                Intent(method),
                FLAG_ONE_SHOT or FLAG_CANCEL_CURRENT or FLAG_IMMUTABLE
            )
            return bundleOf(EXTRA_RESULT to deleteMediaPendingIntent)
        }

        throw UnsupportedOperationException("Calling method \"$method\" is not supported in tests")
    }


    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val tableName = getTableName(uri) ?: return null
        return database.readableDatabase.query(
            tableName,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            sortOrder
        )
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val deleteCount = when (uriMatcher.match(uri)) {
            AUDIO_MEDIA -> database.writableDatabase.delete(
                AudioMediaDatabase.TABLE_MEDIA,
                selection,
                selectionArgs
            )
            AUDIO_MEDIA_ID -> {
                val trackId = ContentUris.parseId(uri)
                database.writableDatabase.delete(
                    AudioMediaDatabase.TABLE_MEDIA,
                    "${BaseColumns._ID} = ?",
                    arrayOf(trackId.toString())
                )
            }
            else -> throw UnsupportedOperationException("Unsupported uri: $uri")
        }

        return deleteCount
    }

    override fun getType(uri: Uri): String? {
        throw UnsupportedOperationException("This operation is not implemented in tests")
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException("This operation is not implemented in tests")
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        throw UnsupportedOperationException("This operation is not implemented in tests")
    }

    override fun shutdown() {
        database.close()
        super.shutdown()
    }

    private fun getTableName(uri: Uri) = when (uriMatcher.match(uri)) {
        AUDIO_MEDIA -> AudioMediaDatabase.TABLE_MEDIA
        AUDIO_ALBUMS -> AudioMediaDatabase.TABLE_ALBUM
        AUDIO_ARTISTS -> AudioMediaDatabase.TABLE_ARTIST
        else -> null
    }
}
