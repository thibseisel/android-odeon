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
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.core.media.MediaId.Builder.encode
import fr.nihilus.music.service.AudioTrack
import fr.nihilus.music.service.extensions.*
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.channels.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
internal class MetadataProducerTest {

    @Test
    fun `Properly extract metadata from track`() = runBlockingTest {
        val sampleMedia = AudioTrack(
            id = MediaId(TYPE_TRACKS, CATEGORY_ALL, 75L),
            title = "Nightmare",
            artist = "Avenged Sevenfold",
            album = "Nightmare",
            mediaUri = Uri.EMPTY,
            iconUri = Uri.parse("file:///Music/Nightmare.mp3"),
            duration = 374648L,
            disc = 1,
            number = 1
        )

        val output = Channel<MediaMetadataCompat>()
        val producer = metadataProducer(DummyBitmapFactory, output)

        try {
            producer.send(sampleMedia)

            val metadata = output.receive()
            metadata.id shouldBe encode(TYPE_TRACKS, CATEGORY_ALL, 75L)

            // Generic display properties.
            assertSoftly(metadata) {
                displayTitle shouldBe "Nightmare"
                displaySubtitle shouldBe "Avenged Sevenfold"
                displayDescription shouldBe "Nightmare"
                duration shouldBe 374648L
                displayIconUri shouldBe "file:///Music/Nightmare.mp3".toUri()
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

    @Test
    fun `When sending two tracks at the same time, then only produce latest`() = runBlockingTest {
        val firstTrackId = MediaId(TYPE_TRACKS, CATEGORY_ALL, 16L)
        val secondTrackId = MediaId(TYPE_TRACKS, CATEGORY_ALL, 42L)

        val tracks = listOf(firstTrackId, secondTrackId).map { id -> audioTrack(id) }

        val output = Channel<MediaMetadataCompat>()
        val producer = metadataProducer(FixedDelayDownloader, output)

        try {
            producer.send(tracks[0])
            producer.send(tracks[1])

            advanceTimeBy(FixedDelayDownloader.DOWNLOAD_TIME)

            val lastTrackMetadata = output.receive()
            lastTrackMetadata.id shouldBe secondTrackId.encoded

        } finally {
            producer.close()
        }
    }

    @Test
    fun `When sending same item two times, then ignore the second one`() = runBlockingTest {
        val trackId = MediaId(TYPE_TRACKS, CATEGORY_ALL, 16L)
        val firstTrack = audioTrack(trackId, "First track")
        val secondTrack = audioTrack(trackId, "Second track")
        val output = Channel<MediaMetadataCompat>()

        val producer = metadataProducer(FixedDelayDownloader, output)

        try {
            producer.send(firstTrack)
            advanceTimeBy(FixedDelayDownloader.DOWNLOAD_TIME / 2)
            producer.send(secondTrack)
            advanceTimeBy(FixedDelayDownloader.DOWNLOAD_TIME / 2)

            val firstMetadata = output.receive()
            assertSoftly(firstMetadata) {
                id shouldBe trackId.encoded
                displayTitle shouldBe "First track"
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

    private fun audioTrack(
        id: MediaId,
        title: String = ""
    ) = AudioTrack(id, title, "", "", Uri.EMPTY, null, 0, 0, 0)
}
