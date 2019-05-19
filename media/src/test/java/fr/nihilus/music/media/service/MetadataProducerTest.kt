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

package fr.nihilus.music.media.service

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import fr.nihilus.music.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.media.MediaId.Builder.encode
import fr.nihilus.music.media.MediaItems
import fr.nihilus.music.media.extensions.*
import fr.nihilus.music.media.fail
import fr.nihilus.music.media.service.MetadataProducerTest.FixedDelayDownloader.DOWNLOAD_TIME
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.debug.junit4.CoroutinesTimeout
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ObsoleteCoroutinesApi
@RunWith(AndroidJUnit4::class)
class MetadataProducerTest {

    @JvmField @Rule
    val coroutineTimeout = CoroutinesTimeout.seconds(5)

    @Test
    fun whenSendingQueueItem_thenExtractItsMetadata() = runBlockingTest {
        val itemMediaId = encode(TYPE_TRACKS, CATEGORY_ALL, 75L)
        val item = QueueItem(
            queueId = 0,
            mediaId = itemMediaId,
            title = "Nightmare",
            subtitle = "Avenged Sevenfold",
            description = "Well, that intro is creepy",
            duration = 374648L,
            iconUri = Uri.parse("file:///Music/Nightmare.mp3")
        )

        val output = Channel<MediaMetadataCompat>()
        val producer = metadataProducer(DummyBitmapFactory, output)

        try {
            producer.send(item)

            val metadata = output.receive()
            assertThat(metadata.id).isEqualTo(itemMediaId)
            assertThat(metadata.displayTitle).isEqualTo("Nightmare")
            assertThat(metadata.displaySubtitle).isEqualTo("Avenged Sevenfold")
            assertThat(metadata.displayDescription).isEqualTo("Well, that intro is creepy")
            assertThat(metadata.duration).isEqualTo(374648L)
            assertThat(metadata.displayIconUri).isEqualTo("file:///Music/Nightmare.mp3")

            // Check that the loaded icon is 320x320 and has a red pixel on top, as defined by DummyBitmapFactory.
            metadata.displayIcon?.let { iconBitmap ->
                assertThat(iconBitmap.width).isEqualTo(320)
                assertThat(iconBitmap.height).isEqualTo(320)
                assertThat(iconBitmap.getPixel(0, 0)).isEqualTo(Color.RED)
            } ?: fail("Metadata should have a bitmap at ${MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON}, but was null.")

        } finally {
            producer.close()
        }
    }

    @Test
    fun whenSendingTwoItemsAtTheSameTime_thenOnlyProduceMetadataOfTheLatest() = runBlockingTest {
        val firstItemId = encode(TYPE_TRACKS, CATEGORY_ALL, 16L)
        val secondItemId = encode(TYPE_TRACKS, CATEGORY_ALL, 42L)

        val items = listOf(firstItemId, secondItemId).mapIndexed { index, mediaId ->
            QueueItem(index.toLong(), mediaId, null, null, null, -1L, Uri.EMPTY)
        }

        val output = Channel<MediaMetadataCompat>()
        val producer = metadataProducer(FixedDelayDownloader, output)

        try {
            producer.send(items[0])
            producer.send(items[1])

            advanceTimeBy(DOWNLOAD_TIME)

            val lastTrackMetadata = output.receive()
            assertThat(lastTrackMetadata.id).isEqualTo(secondItemId)

        } finally {
            producer.close()
        }
    }

    @Test
    fun whenSendingSameItemTwoTimes_thenIgnoreTheSecond() = runBlockingTest {
        val itemId = encode(TYPE_TRACKS, CATEGORY_ALL, 16L)
        val firstItem = QueueItem(1L, itemId, "First item", null, null, 0L, Uri.EMPTY)
        val secondItem = QueueItem(1L, itemId, "Second item", null, null, 0L, Uri.EMPTY)
        val output = Channel<MediaMetadataCompat>()

        val producer = metadataProducer(FixedDelayDownloader, output)

        try {
            producer.send(firstItem)
            advanceTimeBy(DOWNLOAD_TIME / 2)
            producer.send(secondItem)
            advanceTimeBy(DOWNLOAD_TIME / 2)

            val firstMetadata = output.receive()
            assertThat(firstMetadata.id).isEqualTo(itemId)
            assertThat(firstMetadata.displayTitle).isEqualTo("First item")

            advanceTimeBy(DOWNLOAD_TIME / 2)
            assertThat(output.isEmpty).isTrue()

        } finally {
            producer.close()
        }
    }

    /**
     * A fake downloader that create bitmaps instead of fetching them.
     * The created bitmap will always have the specified dimensions
     * and its first top-left pixel will be red, for identification.
     */
    private object DummyBitmapFactory : IconDownloader {
        override suspend fun loadBitmap(iconUri: Uri, width: Int, height: Int): Bitmap? =
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
                if (width > 0 && height > 0) {
                    it.setPixel(0, 0, Color.RED)
                }
            }
    }

    /**
     * A fake downloaded that pretend to take [DOWNLOAD_TIME] to load a `null` bitmap.
     * This fixture is used to simulate long background loads.
     */
    private object FixedDelayDownloader : IconDownloader {
        const val DOWNLOAD_TIME = 100L

        override suspend fun loadBitmap(iconUri: Uri, width: Int, height: Int): Bitmap? {
            delay(DOWNLOAD_TIME)
            return null
        }
    }

    /**
     * Helper function for creating sample queue items.
     */
    private fun QueueItem(
        queueId: Long,
        mediaId: String,
        title: CharSequence?,
        subtitle: CharSequence?,
        description: CharSequence?,
        duration: Long,
        iconUri: Uri?
    ): MediaSessionCompat.QueueItem = MediaSessionCompat.QueueItem(
        MediaDescriptionCompat.Builder()
            .setMediaId(mediaId)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setIconUri(iconUri)
            .setExtras(Bundle(1).apply {
                putLong(MediaItems.EXTRA_DURATION, duration)
            })
            .build(),
        queueId
    )
}
