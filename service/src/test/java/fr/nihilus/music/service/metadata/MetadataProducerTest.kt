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

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.core.media.MediaId.Builder.encode
import fr.nihilus.music.core.media.MediaItems
import fr.nihilus.music.service.extensions.*
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.channels.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class MetadataProducerTest {

    private val sampleMediaDescription by lazy {
        MediaDescription(
            mediaId = encode(TYPE_TRACKS, CATEGORY_ALL, 75L),
            title = "Nightmare",
            subtitle = "Avenged Sevenfold",
            description = "Well, that intro is creepy",
            duration = 374648L,
            iconUri = Uri.parse("file:///Music/Nightmare.mp3")
        )
    }

    @Test
    fun whenSendingQueueItem_thenExtractItsMetadata() = runBlockingTest {
        val output = Channel<MediaMetadataCompat>()
        val producer = metadataProducer(DummyBitmapFactory, output)

        try {
            producer.send(sampleMediaDescription)

            val metadata = output.receive()
            metadata.id shouldBe sampleMediaDescription.mediaId

            // Generic display properties.
            assertSoftly(metadata) {
                displayTitle shouldBe "Nightmare"
                displaySubtitle shouldBe "Avenged Sevenfold"
                displayDescription shouldBe "Well, that intro is creepy"
                duration shouldBe 374648L
                displayIconUri shouldBe "file:///Music/Nightmare.mp3"
            }

            // Specific properties that may be used by some Bluetooth devices.
            assertSoftly(metadata) {
                title shouldBe "Nightmare"
                artist shouldBe "Avenged Sevenfold"
                album shouldBe "Nightmare"
            }

            // Check that the loaded icon is 320x320 and has a red pixel on top, as defined by DummyBitmapFactory.
            val iconBitmap = metadata.albumArt
            iconBitmap.shouldNotBeNull()
            assertSoftly(iconBitmap) {
                width shouldBe 320
                height shouldBe 320
                getPixel(0, 0) shouldBe Color.RED
            }

        } finally {
            producer.close()
        }
    }

    /**
     * Checks that the metadata builder is correctly reused between creation of different metadata.
     * If some properties of the description are missing,
     * then the resulting metadata should not carry over the properties of the previously produced metadata.
     */
    @Test
    fun givenPreviousDescription_whenSendingAnother_thenResultingMetadataShouldNotRetainPropertiesOfThePrevious() = runBlockingTest {
        val emptyDescription = MediaDescriptionCompat.Builder()
            .setMediaId(encode(TYPE_TRACKS, CATEGORY_ALL, 42L))
            .build()

        val output = Channel<MediaMetadataCompat>(Channel.CONFLATED)
        val producer = metadataProducer(DummyBitmapFactory, output)

        try {
            producer.send(sampleMediaDescription)
            producer.send(emptyDescription)
            val metadata = output.receive()

            assertSoftly(metadata) {
                id shouldBe encode(TYPE_TRACKS, CATEGORY_ALL, 42L)
                displayTitle.shouldBeNull()
                displaySubtitle.shouldBeNull()
                displayDescription.shouldBeNull()
                duration shouldBe -1L
                displayIconUri.shouldBeNull()
                albumArt.shouldBeNull()
            }

        } finally {
            producer.close()
        }
    }

    @Test
    fun whenSendingTwoItemsAtTheSameTime_thenOnlyProduceMetadataOfTheLatest() = runBlockingTest {
        val firstItemId = encode(TYPE_TRACKS, CATEGORY_ALL, 16L)
        val secondItemId = encode(TYPE_TRACKS, CATEGORY_ALL, 42L)

        val items = listOf(firstItemId, secondItemId).map { mediaId ->
            MediaDescription(mediaId, null, null, null, -1L, Uri.EMPTY)
        }

        val output = Channel<MediaMetadataCompat>()
        val producer = metadataProducer(FixedDelayDownloader, output)

        try {
            producer.send(items[0])
            producer.send(items[1])

            advanceTimeBy(FixedDelayDownloader.DOWNLOAD_TIME)

            val lastTrackMetadata = output.receive()
            lastTrackMetadata.id shouldBe secondItemId

        } finally {
            producer.close()
        }
    }

    @Test
    fun whenSendingSameItemTwoTimes_thenIgnoreTheSecond() = runBlockingTest {
        val itemId = encode(TYPE_TRACKS, CATEGORY_ALL, 16L)
        val firstItem = MediaDescription(itemId, "First item", null, null, 0L, Uri.EMPTY)
        val secondItem = MediaDescription(itemId, "Second item", null, null, 0L, Uri.EMPTY)
        val output = Channel<MediaMetadataCompat>()

        val producer = metadataProducer(FixedDelayDownloader, output)

        try {
            producer.send(firstItem)
            advanceTimeBy(FixedDelayDownloader.DOWNLOAD_TIME / 2)
            producer.send(secondItem)
            advanceTimeBy(FixedDelayDownloader.DOWNLOAD_TIME / 2)

            val firstMetadata = output.receive()
            assertSoftly(firstMetadata) {
                id shouldBe itemId
                displayTitle shouldBe "First item"
            }

            advanceTimeBy(FixedDelayDownloader.DOWNLOAD_TIME / 2)
            output.shouldBeEmpty()

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
     * A fake downloaded that pretend to take [FixedDelayDownloader.DOWNLOAD_TIME] to load a `null` bitmap.
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
    private fun MediaDescription(
        mediaId: String,
        title: CharSequence?,
        subtitle: CharSequence?,
        description: CharSequence?,
        duration: Long,
        iconUri: Uri?
    ): MediaDescriptionCompat = MediaDescriptionCompat.Builder()
        .setMediaId(mediaId)
        .setTitle(title)
        .setSubtitle(subtitle)
        .setDescription(description)
        .setIconUri(iconUri)
        .setExtras(Bundle(1).apply {
            putLong(MediaItems.EXTRA_DURATION, duration)
        })
        .build()
}
