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
import android.provider.BaseColumns
import android.provider.MediaStore.Audio.Media
import android.provider.MediaStore.Audio.AlbumColumns
import android.support.v4.media.MediaMetadataCompat

private val mediaStoreColumns = arrayOf(BaseColumns._ID, Media.TITLE, Media.ALBUM, Media.ARTIST,
        Media.DURATION, Media.TRACK, Media.TITLE_KEY, Media.ALBUM_KEY, Media.ALBUM_ID,
        Media.ARTIST_ID)

private val artistMediaStoreColumns = arrayOf(Artists._ID, Artists.ARTIST, Artists.ARTIST_KEY,
                Artists.NUMBER_OF_TRACKS)

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
private val mediastoreTracks = arrayOf(
        arrayOf(161, "1741 (The Battle of Cartagena)", "Sunset on the Golden Age", "Alestorm",
                437603L, 1004, """ O71+)OO?1E3-)KO)51C)""", 65, 26),
        arrayOf(309, "The 2nd Law: Isolated System", "The 2nd Law", "Muse", 300042L, 1013,
                """)+EQO)59K?""", 40, 18),
        arrayOf(481, "Dirty Water", "Concrete and Gold", "Foo Fighters", 320914L, 1006,
                """/9KOYU)O1K""", 102, 13),
        arrayOf(86, "Give It Up", "Greatests Hits 30 Anniversary Edition", "AC/DC", 233592L, 1019,
                """59S19OQG""", 7, 5),
        arrayOf(125, "Jailbreak", "Greatests Hits 30 Anniversary Edition", "AC/DC", 276668L, 2014,
                """;)9?+K1)=""", 7, 5),
        arrayOf(294, "Knights of Cydonia", "Black Holes and Revelations", "Muse", 366946L, 1011,
                """=C957OME3-Y/EC9)""", 38, 18),
        arrayOf(219, "A Matter of Time", "Wasting Light", "Foo Fighters", 276140L, 1008,
                """A)OO1KE3O9A1""", 26, 13),
        arrayOf(75, "Nightmare", "Nightmare", "Avenged Sevenfold", 374648L, 1001, """C957OA)K1""",
                6, 4),
        arrayOf(464, "The Pretenders", "Echoes, Silence, Patience & Grace", "Foo Fighters", 266509L,
                1001, """GK1O1C/1KM""", 95, 13),
        arrayOf(477, "Run", "Concrete and Gold", "Foo Fighters", 323424L, 1002, """KQC""", 102, 13)
)

private val albumMediaStoreColumns = arrayOf(Albums._ID, Albums.ALBUM, Albums.ALBUM_KEY, Albums.ARTIST,
                Albums.LAST_YEAR, Albums.NUMBER_OF_SONGS)

private val mediastoreAlbums = arrayOf(
        arrayOf(40, "The 2nd Law", """TODO"""", "Muse", 2012, 1),
        arrayOf(65, "Sunset on the Golden Age", """TODO""", "Alestorm", 2014, 1),
        arrayOf(102, "Concrete and Gold", """TODO""", "Foo Fighters", 2017, 2),
        arrayOf(7, "Greatests Hits 30 Anniversary Edition", """TODO""", "AC/DC", 2010, 2),
        arrayOf(38, "Black Holes and Revelations", """TODO""", "Muse", 2006, 1),
        arrayOf(26, "Wasting Light", """TODO""", "Foo Fighters", 2011, 1),
        arrayOf(6, "Nightname", """TODO""", "Avenged Sevenfold", 2010, 1),
        arrayOf(95, "Echoes, Silence, Patience & Grace", """TODO""", "Foo Fighters", 2007, 1)
)

/**
 * An array containing representative metadata for tracks.
 */
val mockMetadata = arrayOf<MediaMetadataCompat>(
        metadataOf("161", "1741 (The Battle of Cartagena)", "Sunset on the Golden Age", "Alestorm",
                437603L, 1L, 4L, """ O71+)OO?1E3-)KO)51C)""", 65L, 26L),
        metadataOf("309", "The 2nd Law: Isolated System", "The 2nd Law", "Muse", 300042L, 1L, 13L,
                """)+EQO)59K?""", 40L, 18L),
        metadataOf("481", "Dirty Water", "Concrete and Gold", "Foo Fighters", 320914L, 1L, 6L,
                """/9KOYU)O1K""", 102L, 13L),
        metadataOf("86", "Give It Up", "Greatests Hits 30 Anniversary Edition", "AC/DC", 233592L,
                1L, 19L, """59S19OQG""", 7L, 5L),
        metadataOf("125", "Jailbreak", "Greatests Hits 30 Anniversary Edition", "AC/DC",
                276668L, 2L, 14L, """;)9?+K1)=""", 7L, 5L),
        metadataOf("294", "Knights of Cydonia", "Black Holes and Revelations", "Muse", 366946L,
                1L, 11L, """=C957OME3-Y/EC9)""", 38L, 18L),
        metadataOf("219", "A Matter of Time", "Wasting Light", "Foo Fighters", 276140L, 1L, 8L,
                """A)OO1KE3O9A1""", 26L, 13L),
        metadataOf("75", "Nightmare", "Nightmare", "Avenged Sevenfold", 374648L, 1L, 1L,
                """C957OA)K1""", 6L, 4L),
        metadataOf("464", "The Pretenders", "Echoes, Silence, Patience & Grace", "Foo Fighters",
                266509L, 1L, 1L, """GK1O1C/1KM""", 95L, 13L),
        metadataOf("477", "Run", "Concrete and Gold", "Foo Fighters", 323424L, 1L, 2L, """KQC""",
                102, 13)
)

/**
 * Creates a cursor whose tracks information are picked in order from a set of 10 representative tracks.
 */
fun mockTracksCursor(vararg indexes: Int): Cursor {
    val cursor = MatrixCursor(mediaStoreColumns, indexes.size)
    indexes.map { mediastoreTracks[it] }.forEach(cursor::addRow)
    return cursor
}

fun mockAlbumCursor(vararg indexes: Int): Cursor {
    val cursor = MatrixCursor(albumMediaStoreColumns, indexes.size)
    indexes.map { mediastoreAlbums[it] }.forEach(cursor::addRow)
    return cursor
}

private fun mediaUriOf(musicId: String)
        = ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, musicId.toLong())

/**
 * Helper function to create a metadata.
 */
private fun metadataOf(mediaId: String, title: String, album: String, artist: String,
                       duration: Long, discNumber: Long, trackNumber: Long,
                       titleKey: String, albumId: Long, artistId: Long
) = MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
        .putLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER, discNumber)
        .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNumber)
        .putString(MusicDao.CUSTOM_META_TITLE_KEY, titleKey)
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                "content://media/external/audio/albumart/$albumId")
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
                mediaUriOf(mediaId).toString())
        .putLong(MusicDao.CUSTOM_META_ALBUM_ID, albumId)
        .putLong(MusicDao.CUSTOM_META_ARTIST_ID, artistId)
        .build()
