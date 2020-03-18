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
import android.support.v4.media.MediaMetadataCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.core.test.fail
import fr.nihilus.music.service.AudioTrack
import fr.nihilus.music.service.extensions.*
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith

@ObsoleteCoroutinesApi
@RunWith(AndroidJUnit4::class)
class MetadataProducerTest {

    //@JvmField @Rule val coroutineTimeout = CoroutinesTimeout.seconds(5)

    private val sampleMedia by lazy {
        AudioTrack(
            id = MediaId(TYPE_TRACKS, CATEGORY_ALL, 75L),
            title = "Nightmare",
            subtitle = "Avenged Sevenfold",
            album = "Nightmare",
            artist = "Avenged Sevenfold",
            discNumber = 1,
            trackNumber = 1,
            duration = 374648L,
            mediaUri = Uri.parse("file:///Music/Nightmare.mp3"),
            iconUri = Uri.parse("file:///media/artworks/nightmare.png")
        )
    }

    @Test
    fun whenSendingQueueItem_thenExtractItsMetadata() = runBlockingTest {
        val output = Channel<MediaMetadataCompat>()
        val producer = metadataProducer(DummyBitmapFactory, output)

        try {
            producer.send(sampleMedia)

            val metadata = output.receive()
            assertThat(metadata.id).isEqualTo(sampleMedia.id.encoded)

            // Generic display properties.
            assertThat(metadata.displayTitle).isEqualTo("Nightmare")
            assertThat(metadata.displaySubtitle).isEqualTo("Avenged Sevenfold")
            assertThat(metadata.duration).isEqualTo(374648L)
            assertThat(metadata.displayIconUri).isEqualTo("file:///media/artworks/nightmare.png")

            // Specific properties that may be used by some Bluetooth devices.
            assertThat(metadata.title).isEqualTo("Nightmare")
            assertThat(metadata.artist).isEqualTo("Avenged Sevenfold")
            assertThat(metadata.album).isEqualTo("Nightmare")

            // Check that the loaded icon is 320x320 and has a red pixel on top, as defined by DummyBitmapFactory.
            metadata.albumArt?.let { iconBitmap ->
                assertThat(iconBitmap.width).isEqualTo(320)
                assertThat(iconBitmap.height).isEqualTo(320)
                assertThat(iconBitmap.getPixel(0, 0)).isEqualTo(Color.RED)
            } ?: fail("Metadata should have a bitmap at ${MediaMetadataCompat.METADATA_KEY_ALBUM_ART}, but was null.")

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
        val emptyMedia = sampleTrack(MediaId(TYPE_TRACKS, CATEGORY_ALL, 1L))

        val output = Channel<MediaMetadataCompat>(Channel.CONFLATED)
        val producer = metadataProducer(DummyBitmapFactory, output)

        try {
            producer.send(sampleMedia)
            producer.send(emptyMedia)
            val metadata = output.receive()

            assertThat(metadata.id).isEqualTo(emptyMedia.id.encoded)
            assertThat(metadata.displayTitle).isEmpty()
            assertThat(metadata.displaySubtitle).isNull()
            assertThat(metadata.displayDescription).isNull()
            assertThat(metadata.duration).isEqualTo(-1L)
            assertThat(metadata.displayIconUri).isNull()
            assertThat(metadata.albumArt).isNull()

        } finally {
            producer.close()
        }
    }

    @Test
    fun whenSendingTwoItemsAtTheSameTime_thenOnlyProduceMetadataOfTheLatest() = runBlockingTest {
        val firstItemId = MediaId(TYPE_TRACKS, CATEGORY_ALL, 16L)
        val secondItemId = MediaId(TYPE_TRACKS, CATEGORY_ALL, 42L)

        val items = listOf(firstItemId, secondItemId).map { mediaId ->
            sampleTrack(mediaId)
        }

        val output = Channel<MediaMetadataCompat>()
        val producer = metadataProducer(FixedDelayDownloader, output)

        try {
            producer.send(items[0])
            producer.send(items[1])

            advanceTimeBy(FixedDelayDownloader.DOWNLOAD_TIME)

            val lastTrackMetadata = output.receive()
            assertThat(lastTrackMetadata.id).isEqualTo(secondItemId.encoded)

        } finally {
            producer.close()
        }
    }

    @Test
    fun whenSendingSameItemTwoTimes_thenIgnoreTheSecond() = runBlockingTest {
        val itemId = MediaId(TYPE_TRACKS, CATEGORY_ALL, 16L)
        val firstItem = sampleTrack(itemId, "First item", iconUri = Uri.EMPTY)
        val secondItem = sampleTrack(itemId, "Second item", iconUri = Uri.EMPTY)
        val output = Channel<MediaMetadataCompat>()

        val producer = metadataProducer(FixedDelayDownloader, output)

        try {
            producer.send(firstItem)
            advanceTimeBy(FixedDelayDownloader.DOWNLOAD_TIME / 2)
            producer.send(secondItem)
            advanceTimeBy(FixedDelayDownloader.DOWNLOAD_TIME / 2)

            val firstMetadata = output.receive()
            assertThat(firstMetadata.id).isEqualTo(itemId.encoded)
            assertThat(firstMetadata.displayTitle).isEqualTo("First item")

            advanceTimeBy(FixedDelayDownloader.DOWNLOAD_TIME / 2)
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
     * Helper function for creating sample tracks.
     */
    private fun sampleTrack(
        mediaId: MediaId,
        title: String = "",
        iconUri: Uri? = null
    ) = AudioTrack(
        id = mediaId,
        title = title,
        subtitle = null,
        album = "",
        artist = "",
        discNumber = 1,
        trackNumber = 1,
        duration = -1L,
        mediaUri = Uri.EMPTY,
        iconUri = iconUri
    )
}
