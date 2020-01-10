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

package fr.nihilus.music.media.provider

import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import fr.nihilus.music.media.os.MediaStoreDatabase

/**
 * A test fixture for a media provider whose query always fail
 * (i.e. [query] always return a `null` cursor).
 */
internal object FailingMediaStore :
    MediaStoreDatabase {

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun delete(uri: Uri, where: String?, whereArgs: Array<String>?): Int = 0

    override fun registerContentObserver(
        uri: Uri,
        notifyForDescendants: Boolean,
        observer: ContentObserver
    ) = Unit

    override fun unregisterContentObserver(observer: ContentObserver) = Unit
}