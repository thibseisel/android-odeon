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

package fr.nihilus.music.core.ui.actions

import android.Manifest
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ALBUMS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ARTISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_PLAYLISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.media.provider.DeleteTracksResult
import fr.nihilus.music.media.provider.Track
import io.kotest.assertions.extracting
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

private val SAMPLE_TRACKS = listOf(
    Track(
        161,
        "1741 (The Battle of Cartagena)",
        "Alestorm",
        "Sunset on the Golden Age",
        437603,
        1,
        4,
        "",
        null,
        1466283480,
        26,
        65,
        17_506_481
    ),
    Track(
        309,
        "The 2nd Law: Isolated System",
        "Muse",
        "The 2nd Law",
        300042,
        1,
        13,
        "",
        null,
        1439653800,
        18,
        40,
        12_075_967
    ),
    Track(
        481,
        "Dirty Water",
        "Foo Fighters",
        "Concrete and Gold",
        320914,
        1,
        6,
        "",
        null,
        1506374520,
        13,
        102,
        12_912_282
    ),
    Track(
        48,
        "Give It Up",
        "AC/DC",
        "Greatest Hits 30 Anniversary Edition",
        233592,
        1,
        19,
        "",
        null,
        1455310080,
        5,
        7,
        5_716_578
    ),
    Track(
        125,
        "Jailbreak",
        "AC/DC",
        "Greatest Hits 30 Anniversary Edition",
        276668,
        2,
        14,
        "",
        null,
        1455310140,
        5,
        7,
        6_750_404
    ),
    Track(
        294,
        "Knights of Cydonia",
        "Muse",
        "Black Holes and Revelations",
        366946,
        1,
        11,
        "",
        null,
        1414880700,
        18,
        38,
        11_746_572
    ),
    Track(
        219,
        "A Matter of Time",
        "Foo Fighters",
        "Wasting Light",
        276140,
        1,
        8,
        "",
        null,
        1360677660,
        13,
        26,
        11_149_678
    ),
    Track(
        75,
        "Nightmare",
        "Avenged Sevenfold",
        "Nightmare",
        374648,
        1,
        1,
        "",
        null,
        1439590380,
        4,
        6,
        10_828_662
    ),
    Track(
        464,
        "The Pretenders",
        "Foo Fighters",
        "Echoes, Silence, Patience & Grace",
        266509,
        1,
        1,
        "",
        null,
        1439653740,
        13,
        95,
        4_296_041
    ),
    Track(
        477,
        "Run",
        "Foo Fighters",
        "Concrete and Gold",
        323424,
        1,
        2,
        "",
        null,
        1506374520,
        13,
        102,
        13_012_576
    )
)

/**
 * Verify behavior of [DeleteTracksAction].
 */
internal class DeleteTracksActionTest {

    @Test
    fun `Given invalid track media ids, when deleting then fail with IAE`() = runTest {
        val dao = InMemoryTrackDao()
        val deleteAction = DeleteTracksAction(dao)

        val invalidTrackIds = listOf(
            MediaId(TYPE_TRACKS, CATEGORY_ALL),
            MediaId(TYPE_ALBUMS, "13"),
            MediaId(TYPE_ARTISTS, "78"),
            MediaId(TYPE_PLAYLISTS, "9")
        )

        for (mediaId in invalidTrackIds) {
            shouldThrow<IllegalArgumentException> {
                deleteAction(listOf(mediaId))
            }
        }
    }

    @Test
    fun `When deleting tracks then remove records from dao`() = runTest {
        val dao = InMemoryTrackDao(initial = SAMPLE_TRACKS)
        val deleteAction = DeleteTracksAction(dao)

        val deleteResult = deleteAction(
            mediaIds = listOf(
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 161),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 48),
                MediaId(TYPE_TRACKS, CATEGORY_ALL, 75)
            )
        )

        deleteResult.shouldBeInstanceOf<DeleteTracksResult.Deleted>()
        deleteResult.count shouldBe 3

        val savedTracks = dao.tracks.first()
        savedTracks.size shouldBe 7

        extracting(savedTracks) { id }.also {
            it shouldNotContain 161
            it shouldNotContain 48
            it shouldNotContain 75
        }
    }

    @Test
    fun `Given denied permission, when deleting tracks then return RequiresPermission`() = runTest {
        val deniedDao = InMemoryTrackDao(permissionGranted = false)
        val deleteAction = DeleteTracksAction(deniedDao)

        val targetTrackIds = listOf(
            MediaId(TYPE_TRACKS, CATEGORY_ALL, 161),
            MediaId(TYPE_TRACKS, CATEGORY_ALL, 464)
        )

        val result = deleteAction(targetTrackIds)
        result.shouldBeInstanceOf<DeleteTracksResult.RequiresPermission>()
        result.permission shouldBe Manifest.permission.WRITE_EXTERNAL_STORAGE
    }
}
