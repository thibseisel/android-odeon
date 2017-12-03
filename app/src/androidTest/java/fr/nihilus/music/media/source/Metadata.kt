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

private val mediaStoreColumns = arrayOf(BaseColumns._ID, Media.TITLE, Media.ALBUM, Media.ARTIST,
        Media.DURATION, Media.TRACK, Media.TITLE_KEY, Media.ALBUM_ID, Media.ARTIST_ID,
        Media.DATE_ADDED)

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
        arrayOf(161L, "1741 (The Battle of Cartagena)", "Sunset on the Golden Age", "Alestorm",
                437603L, 1004, """ O71+)OO?1E3-)KO)51C)""", 65L, 26L, 1000003673L),
        arrayOf(309L, "The 2nd Law: Isolated System", "The 2nd Law", "Muse", 300042L, 1013,
                """)+EQO)59K?""", 40L, 18L, 1000001838L),
        arrayOf(481L, "Dirty Water", "Concrete and Gold", "Foo Fighters", 320914L, 1006,
                """/9KOYU)O1K""", 102L, 13L, 1000009113L),
        arrayOf(86L, "Give It Up", "Greatests Hits 30 Anniversary Edition", "AC/DC", 233592L, 1019,
                """59S19OQG""", 7L, 5L, 1000003095L),
        arrayOf(125L, "Jailbreak", "Greatests Hits 30 Anniversary Edition", "AC/DC", 276668L, 2014,
                """;)9?+K1)=""", 7L, 5L, 1000003503L),
        arrayOf(294L, "Knights of Cydonia", "Black Holes and Revelations", "Muse", 366946L, 1011,
                """=C957OME3-Y/EC9)""", 38L, 18L, 1000001838L),
        arrayOf(219L, "A Matter of Time", "Wasting Light", "Foo Fighters", 276140L, 1008,
                """A)OO1KE3O9A1""", 26L, 13L, 1000002658L),
        arrayOf(75L, "Nightmare", "Nightmare", "Avenged Sevenfold", 374648L, 1001, """C957OA)K1""",
                6L, 4L, 1000003075L),
        arrayOf(464, "The Pretenders", "Echoes, Silence, Patience & Grace", "Foo Fighters", 266509L,
                1001, """GK1O1C/1KM""", 95L, 13L, 1000001624L),
        arrayOf(477L, "Run", "Concrete and Gold", "Foo Fighters", 323424L, 1002, """KQC""",
                102L, 13L, 1000007047L)
)

/**
 * An array containing representative metadata for tracks.
 */
val mockMetadata = arrayOf<MediaMetadataCompat>(
        metadataOf("161", "1741 (The Battle of Cartagena)", "Sunset on the Golden Age", "Alestorm",
                437603L, 1L, 4L, """ O71+)OO?1E3-)KO)51C)""", 65L, 26L, 1000003673L),
        metadataOf("309", "The 2nd Law: Isolated System", "The 2nd Law", "Muse", 300042L, 1L, 13L,
                """)+EQO)59K?""", 40L, 18L, 1000001838L),
        metadataOf("481", "Dirty Water", "Concrete and Gold", "Foo Fighters", 320914L, 1L, 6L,
                """/9KOYU)O1K""", 102L, 13L, 1000009113L),
        metadataOf("86", "Give It Up", "Greatests Hits 30 Anniversary Edition", "AC/DC", 233592L,
                1L, 19L, """59S19OQG""", 7L, 5L, 1000003095L),
        metadataOf("125", "Jailbreak", "Greatests Hits 30 Anniversary Edition", "AC/DC",
                276668L, 2L, 14L, """;)9?+K1)=""", 7L, 5L, 1000003503L),
        metadataOf("294", "Knights of Cydonia", "Black Holes and Revelations", "Muse", 366946L,
                1L, 11L, """=C957OME3-Y/EC9)""", 38L, 18L, 1000001838L),
        metadataOf("219", "A Matter of Time", "Wasting Light", "Foo Fighters", 276140L, 1L, 8L,
                """A)OO1KE3O9A1""", 26L, 13L, 1000002658L),
        metadataOf("75", "Nightmare", "Nightmare", "Avenged Sevenfold", 374648L, 1L, 1L,
                """C957OA)K1""", 6L, 4L, 1000003075L),
        metadataOf("464", "The Pretenders", "Echoes, Silence, Patience & Grace", "Foo Fighters",
                266509L, 1L, 1L, """GK1O1C/1KM""", 95L, 13L, 1000001624L),
        metadataOf("477", "Run", "Concrete and Gold", "Foo Fighters", 323424L, 1L, 2L, """KQC""",
                102, 13, 1000007047L)
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
private val albumMediaStoreColumns = arrayOf(Albums._ID, Albums.ALBUM, Albums.ALBUM_KEY, Albums.ARTIST,
        Albums.LAST_YEAR, Albums.NUMBER_OF_SONGS)

private val mediaStoreAlbums = arrayOf(
        arrayOf(40L, "The 2nd Law", """C/?)U""", "Muse", 2012, 1),
        arrayOf(65L, "Sunset on the Golden Age", """MQCM1OECO715E?/1C)51""", "Alestorm", 2014, 1),
        arrayOf(102L, "Concrete and Gold", """-EC-K1O1)C/5E?/""", "Foo Fighters", 2017, 2),
        arrayOf(7L, "Greatests Hits 30 Anniversary Edition", """5K1)O1MOM79OM)CC9S1KM)KY1/9O9EC""",
                "AC/DC", 2010, 2),
        arrayOf(38L, "Black Holes and Revelations", """+?)-=7E?1M)C/K1S1?)O9ECM""", "Muse", 2006, 1),
        arrayOf(26L, "Wasting Light", """U)MO9C5?957O""", "Foo Fighters", 2011, 1),
        arrayOf(6L, "Nightname", """C957OA)K1""", "Avenged Sevenfold", 2010, 1),
        arrayOf(95L, "Echoes, Silence, Patience & Grace", """1-7E1MM9?1C-1G)O91C-1
       5K)-1""", "Foo Fighters", 2007, 1)
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
private val artistMediaStoreColumns = arrayOf(Artists._ID, Artists.ARTIST, Artists.ARTIST_KEY,
        Artists.NUMBER_OF_TRACKS)

private val mediaStoreArtists = arrayOf(
        arrayOf(5L, "AC/DC", """)-
                      /-""", 2),
        arrayOf(26L, "Alestorm", """)?1MOEKA""", 1),
        arrayOf(4L, "Avenged Sevenfold", """)S1C51/M1S1C3E?/""", 1),
        arrayOf(13L, "Foo Fighters", """3EE3957O1KM""", 4),
        arrayOf(18L, "Muse", """AQM1""", 2)
)

/**
 * Creates a custor containing artists whose informations are picked in order from a set of
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

fun mediaUriOf(musicId: String)
        = ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, musicId.toLong())

fun artUriOf(albumId: Long)
        = Uri.parse("content://media/external/audio/albumart/$albumId")

/**
 * Helper function to create a metadata.
 */
private fun metadataOf(mediaId: String, title: String, album: String, artist: String,
                       duration: Long, discNumber: Long, trackNumber: Long,
                       titleKey: String, albumId: Long, artistId: Long, dateAdded: Long
) = MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
        .putLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER, discNumber)
        .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNumber)
        .putString(MusicDao.METADATA_KEY_TITLE_KEY, titleKey)
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                "content://media/external/audio/albumart/$albumId")
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
                mediaUriOf(mediaId).toString())
        .putLong(MusicDao.METADATA_KEY_ALBUM_ID, albumId)
        .putLong(MusicDao.METADATA_KEY_ARTIST_ID, artistId)
        .putLong(MusicDao.METADATA_KEY_DATE, dateAdded)
        .build()

private fun mediaDescriptionOf(mediaId: String, title: String, subtitle: String,
                               description: String?, iconUri: Uri?, extras: Bundle?
) = MediaDescriptionCompat.Builder()
        .setMediaId(mediaId)
        .setTitle(title)
        .setSubtitle(subtitle)
        .setDescription(description)
        .setIconUri(iconUri)
        .setExtras(extras)
        .build()