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

package fr.nihilus.music.service.metadata

import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import fr.nihilus.music.service.ServiceScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import javax.inject.Inject

private val EMPTY_METADATA = MediaMetadataCompat.Builder().build()

@ServiceScoped
internal class OdeonMetadataProvider @Inject constructor(
    scope: CoroutineScope,
    downloader: IconDownloader
) : MediaSessionConnector.MediaMetadataProvider {

    private val nowPlaying = ConflatedBroadcastChannel<MediaMetadataCompat>(EMPTY_METADATA)
    private val producer = scope.metadataProducer(downloader, nowPlaying)

    override fun getMetadata(player: Player): MediaMetadataCompat {
        val description = player.currentTag as? MediaDescriptionCompat?

        return if (description != null) {
            producer.offer(description)
            nowPlaying.value
        } else {
            EMPTY_METADATA
        }
    }
}