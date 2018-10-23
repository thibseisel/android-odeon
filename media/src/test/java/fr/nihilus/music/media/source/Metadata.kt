/*
 * Copyright 2017 Thibault Seisel
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

package fr.nihilus.music.media.source

import android.content.ContentUris
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.MediaStore.Audio.*
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import fr.nihilus.music.media.extensions.*

private val mediaStoreColumns = arrayOf(
    BaseColumns._ID, Media.TITLE, Media.ALBUM, Media.ARTIST,
    Media.DURATION, Media.TRACK, Media.TITLE_KEY, Media.ALBUM_ID, Media.ARTIST_ID,
    Media.DATE_ADDED
)

/**
 * A representative sample of 10 songs metadata.
 * Each value index correspond to the key specified in [mediaStoreColumns].
 *
 * Covered cases :
 * - 1 track whose name starts with a number (N°0)
 * - 2 tracks from the same artist but different album (N°1 and 5)
 * - 2 tracks from the same album and artist, but different disc number (N°3 and 4)
 * - 1 track whose title is the same as the album (N°7)
 * - 4 tracks from th same artist, but different albums (N°2, 6, 8 and 9)
 */
val mediaStoreTracks = arrayOf(
    arrayOf(
        161L,
        "1741 (The Battle of Cartagena)",
        "Sunset on the Golden Age",
        "Alestorm",
        437603L,
        1004,
        """ O71+)OO?1E3-)KO)51C)""",
        65L,
        26L,
        1000003673L
    ),
    arrayOf(
        309L, "The 2nd Law: Isolated System", "The 2nd Law", "Muse", 300042L, 1013,
        """)+EQO)59K?""", 40L, 18L, 1000001838L
    ),
    arrayOf(
        481L, "Dirty Water", "Concrete and Gold", "Foo Fighters", 320914L, 1006,
        """/9KOYU)O1K""", 102L, 13L, 1000009113L
    ),
    arrayOf(
        86L, "Give It Up", "Greatests Hits 30 Anniversary Edition", "AC/DC", 233592L, 1019,
        """59S19OQG""", 7L, 5L, 1000003095L
    ),
    arrayOf(
        125L, "Jailbreak", "Greatests Hits 30 Anniversary Edition", "AC/DC", 276668L, 2014,
        """;)9?+K1)=""", 7L, 5L, 1000003503L
    ),
    arrayOf(
        294L, "Knights of Cydonia", "Black Holes and Revelations", "Muse", 366946L, 1011,
        """=C957OME3-Y/EC9)""", 38L, 18L, 1000001838L
    ),
    arrayOf(
        219L, "A Matter of Time", "Wasting Light", "Foo Fighters", 276140L, 1008,
        """A)OO1KE3O9A1""", 26L, 13L, 1000002658L
    ),
    arrayOf(
        75L,
        "Nightmare",
        "Nightmare",
        "Avenged Sevenfold",
        374648L,
        1001,
        """C957OA)K1""",
        6L,
        4L,
        1000003075L
    ),
    arrayOf(
        464, "The Pretenders", "Echoes, Silence, Patience & Grace", "Foo Fighters", 266509L,
        1001, """GK1O1C/1KM""", 95L, 13L, 1000001624L
    ),
    arrayOf(
        477L, "Run", "Concrete and Gold", "Foo Fighters", 323424L, 1002, """KQC""",
        102L, 13L, 1000007047L
    )
)

/**
 * An array containing representative metadata for tracks.
 */
val mockMetadata = arrayOf<MediaMetadataCompat>(
    buildMetadata {
        id = "161"
        title = "1741 (The Battle of Cartagena)"
        album = "Sunset on the Golden Age"
        artist = "Alestorm"
        duration = 437603L
        discNumber = 1L
        trackNumber = 4L
        titleKey = """ O71+)OO?1E3-)KO)51C)"""
        albumArtUri = "file:///storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509626970548"
        mediaUri = mediaUriOf("161").toString()
        albumId = 65L
        artistId = 26L
        availabilityDate = 1000003673L
    },
    buildMetadata {
        id = "309"
        title = "The 2nd Law: Isolated System"
        album = "The 2nd Law"
        artist = "Muse"
        duration = 300042L
        discNumber = 1L
        trackNumber = 13L
        titleKey = """)+EQO)59K?"""
        albumArtUri = "file:///storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627051019"
        mediaUri = mediaUriOf("309").toString()
        albumId = 40L
        artistId = 18L
        availabilityDate = 1000001838L
    },
    buildMetadata {
        id = "481"
        title = "Dirty Water"
        album = "Concrete and Gold"
        artist = "Foo Fighters"
        duration = 320914L
        discNumber = 1L
        trackNumber = 6L
        titleKey = """/9KOYU)O1K"""
        albumArtUri = "file:///storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627413029"
        mediaUri = mediaUriOf("481").toString()
        albumId = 102L
        artistId = 13L
        availabilityDate = 1000009113L
    },
    buildMetadata {
        id = "86"
        title = "Give It Up"
        album = "Greatests Hits 30 Anniversary Edition"
        artist = "AC/DC"
        duration = 233592L
        discNumber = 1L
        trackNumber = 19L
        titleKey = """59S19OQG"""
        albumArtUri = "file:///storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509626951708"
        mediaUri = mediaUriOf("86").toString()
        albumId = 7L
        artistId = 5L
        availabilityDate = 1000003095L
    },
    buildMetadata {
        id = "125"
        title = "Jailbreak"
        album = "Greatests Hits 30 Anniversary Edition"
        artist = "AC/DC"
        duration = 276668L
        discNumber = 2L
        trackNumber = 14L
        titleKey = """;)9?+K1)="""
        albumArtUri = "file:///storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509626951708"
        mediaUri = mediaUriOf("125").toString()
        albumId = 7L
        artistId = 5L
        availabilityDate = 1000003503L
    },
    buildMetadata {
        id = "294"
        title = "Knights of Cydonia"
        album = "Black Holes and Revelations"
        artist = "Muse"
        duration = 366946L
        discNumber = 1L
        trackNumber = 11L
        titleKey = """=C957OME3-Y/EC9)"""
        albumArtUri = "file:///storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627043599"
        mediaUri = mediaUriOf("294").toString()
        albumId = 38L
        artistId = 18L
        availabilityDate = 1000001838L
    },
    buildMetadata {
        id = "219"
        title = "A Matter of Time"
        album = "Wasting Light"
        artist = "Foo Fighters"
        duration = 276140L
        discNumber = 1L
        trackNumber = 8L
        titleKey = """A)OO1KE3O9A1"""
        albumArtUri = "file:///storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627009356"
        mediaUri = mediaUriOf("219").toString()
        albumId = 26L
        artistId = 13L
        availabilityDate = 1000002658L
    },
    buildMetadata {
        id = "75"
        title = "Nightmare"
        album = "Nightmare"
        artist = "Avenged Sevenfold"
        duration = 374648L
        discNumber = 1L
        trackNumber = 1L
        titleKey = """C957OA)K1"""
        albumArtUri = "file:///storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509626949249"
        mediaUri = mediaUriOf("75").toString()
        albumId = 6L
        artistId = 4L
        availabilityDate = 1000003075L
    },
    buildMetadata {
        id = "464"
        title = "The Pretenders"
        album = "Echoes, Silence, Patience & Grace"
        artist = "Foo Fighters"
        duration = 266509L
        discNumber = 1L
        trackNumber = 1L
        titleKey = """GK1O1C/1KM"""
        albumArtUri = "file:///storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627124517"
        mediaUri = mediaUriOf("464").toString()
        albumId = 95L
        artistId = 13L
        availabilityDate = 1000001624L
    },
    buildMetadata {
        id = "477"
        title = "Run"
        album = "Concrete and Gold"
        artist = "Foo Fighters"
        duration = 323424L
        discNumber = 1L
        trackNumber = 2L
        titleKey = """KQC"""
        albumArtUri = "file:///storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627413029"
        mediaUri = mediaUriOf("477").toString()
        albumId = 102
        artistId = 13
        availabilityDate = 1000007047L
    }
)

/**
 * Creates a cursor whose tracks information is picked in order from a set of 10 representative tracks.
 *
 * @param indexes Index of tracks to pick from the set in order. All values must be below 10.
 * @return a cursor containing the picked tracks sorted in the order of index declaration.
 */
fun mockTracksCursor(vararg indexes: Int): Cursor {
    val cursor = MatrixCursor(mediaStoreColumns, indexes.size)
    indexes.map { mediaStoreTracks[it] }.forEach(cursor::addRow)
    return cursor
}

// ALBUMS
private val albumMediaStoreColumns = arrayOf(
    Albums._ID, Albums.ALBUM, Albums.ALBUM_KEY, Albums.ARTIST,
    Albums.LAST_YEAR, Albums.NUMBER_OF_SONGS, Albums.ALBUM_ART
)

private val mediaStoreAlbums = arrayOf(
    arrayOf(
        40L,
        "The 2nd Law",
        """C/?)U""",
        "Muse",
        2012,
        1,
        "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627051019"
    ),
    arrayOf(
        65L,
        "Sunset on the Golden Age",
        """MQCM1OECO715E?/1C)51""",
        "Alestorm",
        2014,
        1,
        "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509626970548"
    ),
    arrayOf(
        102L,
        "Concrete and Gold",
        """-EC-K1O1)C/5E?/""",
        "Foo Fighters",
        2017,
        2,
        "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627413029"
    ),
    arrayOf(
        7L,
        "Greatests Hits 30 Anniversary Edition",
        """5K1)O1MOM79OM)CC9S1KM)KY1/9O9EC""",
        "AC/DC",
        2010,
        2,
        "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509626951708"
    ),
    arrayOf(
        38L,
        "Black Holes and Revelations",
        """+?)-=7E?1M)C/K1S1?)O9ECM""",
        "Muse",
        2006,
        1,
        "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627043599"
    ),
    arrayOf(
        26L,
        "Wasting Light",
        """U)MO9C5?957O""",
        "Foo Fighters",
        2011,
        1,
        "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627009356"
    ),
    arrayOf(
        6L,
        "Nightmare",
        """C957OA)K1""",
        "Avenged Sevenfold",
        2010,
        1,
        "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509626949249"
    ),
    arrayOf(
        95L,
        "Echoes, Silence, Patience & Grace",
        """1-7E1MM9?1C-1G)O91C-1
       5K)-1""",
        "Foo Fighters",
        2007,
        1,
        "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627124517"
    )
)

/**
 * Creates a cursor containing albums whose information is picked in order from a set of
 * 8 representative albums.
 * Album metadata are coherent with track metadata: each track has a corresponding album in this set.
 *
 * @param indexes Index of albums to pick from the set in order. All values must be below 8.
 * @return a cursor containing the picked albums sorted in the order of index declaration.
 */
fun mockAlbumCursor(vararg indexes: Int): Cursor {
    val cursor = MatrixCursor(albumMediaStoreColumns, indexes.size)
    indexes.map(mediaStoreAlbums::get).forEach(cursor::addRow)
    return cursor
}

// ARTISTS
private val artistMediaStoreColumns = arrayOf(
    Artists._ID, Artists.ARTIST, Artists.ARTIST_KEY,
    Artists.NUMBER_OF_TRACKS
)

private val mediaStoreArtists = arrayOf(
    arrayOf(
        5L, "AC/DC", """)-
                      /-""", 2
    ),
    arrayOf(26L, "Alestorm", """)?1MOEKA""", 1),
    arrayOf(4L, "Avenged Sevenfold", """)S1C51/M1S1C3E?/""", 1),
    arrayOf(13L, "Foo Fighters", """3EE3957O1KM""", 4),
    arrayOf(18L, "Muse", """AQM1""", 2)
)

/**
 * Creates a custom containing artists whose information are picked in order from a set of
 * 5 representative artists.
 * Artist metadata are coherent with track metadata: each track has a corresponding artist in this set.
 *
 * @param indexes Index of artists to pick from the set in order. All values must be below 5.
 * @return a cursor containing the picked artists sorted in the order of index declaration.
 */
fun mockArtistCursor(vararg indexes: Int): Cursor {
    val cursor = MatrixCursor(artistMediaStoreColumns, indexes.size)
    indexes.map(mediaStoreArtists::get).forEach(cursor::addRow)
    return cursor
}

fun mediaUriOf(musicId: String): Uri =
    ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, musicId.toLong())

private fun mediaDescriptionOf(
    mediaId: String, title: String, subtitle: String,
    description: String?, iconUri: Uri?, extras: Bundle?
) = MediaDescriptionCompat.Builder()
    .setMediaId(mediaId)
    .setTitle(title)
    .setSubtitle(subtitle)
    .setDescription(description)
    .setIconUri(iconUri)
    .setExtras(extras)
    .build()

private inline fun buildMetadata(builder: MediaMetadataCompat.Builder.() -> Unit) =
    MediaMetadataCompat.Builder().apply(builder).build()