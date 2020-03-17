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

package fr.nihilus.music.service

import android.Manifest
import android.support.v4.media.MediaBrowserCompat.MediaItem
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.os.PermissionDeniedException
import fr.nihilus.music.core.test.stub
import fr.nihilus.music.service.browser.BrowserTree
import fr.nihilus.music.service.browser.SearchQuery
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A test implementation of BrowserTree that throws [PermissionDeniedException]
 * when [granted] is `false` to simulate missing external storage permissions.
 *
 * Permissions could be revoked or granted at anytime by setting the value of [granted].
 * Revoking a permission makes currently observed flows throw [PermissionDeniedException].
 * Otherwise, that flow will periodically emit an empty list to simulate updates.
 *
 * @property granted Whether permission to read external storage is granted.
 * When permission is not granted, all operations throw [PermissionDeniedException].
 */
internal class PermissionBrowserTree(
    var granted: Boolean
) : BrowserTree {

    override fun getChildren(parentId: MediaId): Flow<List<MediaItem>> = flow {
        while (true) {
            if (granted) emit(emptyList<MediaItem>())
            else throw PermissionDeniedException(Manifest.permission.READ_EXTERNAL_STORAGE)
            delay(1000)
        }
    }

    override suspend fun getItem(itemId: MediaId): MediaItem? = stub()

    override suspend fun search(query: SearchQuery): List<MediaItem> = stub()
}