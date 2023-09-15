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
import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Builds a new [Uri] by appending the given [id] to the end of this uri's path.
 * @see ContentUris.withAppendedId
 */
internal fun Uri.withAppendedId(id: Long) = ContentUris.withAppendedId(this, id)

/**
 * Builds a flow that emits whenever content at the given [contentUri] changes.
 * @param contentUri URI to watch for changes. This can be a specific row URI, or a base URI
 * for a whole class of content.
 */
internal fun ContentResolver.observeContentChanges(contentUri: Uri): Flow<Unit> {
    require(contentUri.scheme == ContentResolver.SCHEME_CONTENT)

    return callbackFlow {
        // Trigger initial load.
        send(Unit)

        // Register an observer until flow is cancelled, and reload whenever it is notified.
        val observer = ChannelContentObserver(channel)
        registerContentObserver(contentUri, true, observer)

        awaitClose { unregisterContentObserver(observer) }
    }
}

/**
 * A [ContentObserver] that sends its change notifications to a [Kotlin Channel][SendChannel].
 */
private class ChannelContentObserver(
    private val channel: SendChannel<Unit>
) : ContentObserver(null) {

    override fun deliverSelfNotifications(): Boolean = false

    override fun onChange(selfChange: Boolean, uri: Uri?) = onChange(selfChange)

    override fun onChange(selfChange: Boolean) {
        channel.trySend(Unit)
    }
}
