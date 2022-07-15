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

package fr.nihilus.music.service.metadata

import android.support.v4.media.MediaMetadataCompat
import fr.nihilus.music.media.AudioTrack
import fr.nihilus.music.service.extensions.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach

/**
 * The maximum width/height for the icon of the currently playing metadata.
 */
private const val ICON_MAX_SIZE = 320

/**
 * Starts a coroutine that transforms received queue items into [MediaMetadataCompat] instances
 * suitable for being set as the MediaSession's current metadata.
 *
 * If an item is received while the previous one is being processed,
 * then its processing is cancelled and the newly received item is processed instead.
 * This prevents from wasting resource preparing metadata for an item that is no longer valid.
 *
 * @param downloader The downloader used to load icons associated with each metadata.
 * @param metadata Channel to which metadata should be sent when ready.
 */
@OptIn(ObsoleteCoroutinesApi::class)
internal fun CoroutineScope.metadataProducer(
    downloader: IconDownloader,
    metadata: SendChannel<MediaMetadataCompat>
): SendChannel<AudioTrack> = actor(
    capacity = Channel.CONFLATED,
    start = CoroutineStart.LAZY
) {
    val builder = MediaMetadataCompat.Builder()
    var currentlyPlayingItem = receive()
    var updateJob = scheduleMetadataUpdate(downloader, currentlyPlayingItem, builder, metadata)

    consumeEach { description ->
        if (currentlyPlayingItem.id != description.id) {
            currentlyPlayingItem = description

            updateJob.cancel()
            updateJob = scheduleMetadataUpdate(downloader, description, builder, metadata)
        }
    }
}

/**
 * Extract track metadata from the provided [media description][track] and load its icon.
 * The launched coroutine supports cancellation.
 *
 * @param downloader The downloader used to load the icon associated with the metadata.
 * @param track The track from which metadata should be extracted.
 * @param builder A metadata builder that can be reused to create different [MediaMetadataCompat] instances.
 * @param output Send metadata to this channel when ready.
 */
private fun CoroutineScope.scheduleMetadataUpdate(
    downloader: IconDownloader,
    track: AudioTrack,
    builder: MediaMetadataCompat.Builder,
    output: SendChannel<MediaMetadataCompat>
): Job = launch {
    val trackIcon = track.iconUri?.let {
        downloader.loadBitmap(it, ICON_MAX_SIZE, ICON_MAX_SIZE)
    }

    val metadata = builder.apply {
        id = track.id.encoded
        title = track.title
        displayTitle = track.title
        displaySubtitle = track.artist
        displayDescription = track.album
        displayIconUri = track.iconUri
        displayIcon = trackIcon
        albumArt = trackIcon
        duration = track.duration
        artist = track.artist
        album = track.album
    }.build()

    output.send(metadata)
}
