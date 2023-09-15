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

import android.content.ContentResolver
import android.net.Uri
import android.provider.BaseColumns
import android.provider.MediaStore

/**
 * Defines contract for accessing non-SDK features of Android's [MediaStore].
 *
 * While not being officially exposed as [MediaStore] constants, the available
 * media collections have the advantage of being available on all Android versions
 * and to external applications without permissions.
 */
internal object MediaStoreInternals {
    /**
     * Contains artworks of audio albums.
     */
    object AudioThumbnails : BaseColumns {
        /**
         * The `content://` style URI for the "primary" external storage volume.
         */
        val CONTENT_URI: Uri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(MediaStore.AUTHORITY)
            .appendPath("external")
            .appendEncodedPath("audio/albumart")
            .build()

        /**
         * Id of the album illustrated by this thumbnail.
         *
         * Type: `String`
         */
        @Suppress("unused")
        const val ALBUM_ID = "_album_id"

        /**
         * Path to the thumbnail file on disk.
         * Applications may not have the permission to access this path;
         * you may only use this value to determine if an album has an artwork.
         *
         * Type: `String`
         */
        @Suppress("unused")
        const val DATA = "_data"
    }
}
