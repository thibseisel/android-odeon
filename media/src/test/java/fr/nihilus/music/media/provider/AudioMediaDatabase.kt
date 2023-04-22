/*
 * Copyright 2022 Thibault Seisel
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

package fr.nihilus.music.media.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.MediaStore
import java.io.File

/**
 * In-memory database used as a test double for the [MediaStore] content.
 */
@Suppress("DEPRECATION")
internal class AudioMediaDatabase(context: Context) : SQLiteOpenHelper(context, null, null, 1) {
    companion object {
        const val TABLE_MEDIA = "media"
        const val TABLE_ALBUM = "album"
        const val TABLE_ARTIST = "artist"
    }

    override fun onCreate(db: SQLiteDatabase) {
        createSchema(db)
        insertSampleArtists(db)
        insertSampleAlbums(db)
        insertSampleMedia(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    private fun createSchema(db: SQLiteDatabase) {
        db.execSQL(
            """
                CREATE TABLE $TABLE_MEDIA (
                    ${MediaStore.Audio.Media._ID} INTEGER PRIMARY KEY,
                    ${MediaStore.Audio.Media.TITLE} TEXT,
                    ${MediaStore.Audio.Media.TITLE_KEY} TEXT,
                    ${MediaStore.Audio.Media.ALBUM_ID} INTEGER NOT NULL,
                    ${MediaStore.Audio.Media.ALBUM} TEXT,
                    ${MediaStore.Audio.Media.ALBUM_KEY} TEXT,
                    ${MediaStore.Audio.Media.ARTIST_ID} INTEGER NOT NULL,
                    ${MediaStore.Audio.Media.ARTIST} TEXT,
                    ${MediaStore.Audio.Media.ARTIST_KEY} TEXT,
                    ${MediaStore.Audio.Media.DURATION} INTEGER,
                    ${MediaStore.Audio.Media.TRACK} INTEGER,
                    ${MediaStore.Audio.Media.COMPOSER} TEXT,
                    ${MediaStore.Audio.Media.BOOKMARK} INTEGER,
                    ${MediaStore.Audio.Media.IS_MUSIC} INTEGER NOT NULL,
                    ${MediaStore.Audio.Media.IS_ALARM} INTEGER NOT NULL,
                    ${MediaStore.Audio.Media.IS_NOTIFICATION} INTEGER NOT NULL,
                    ${MediaStore.Audio.Media.IS_PODCAST} INTEGER NOT NULL,
                    ${MediaStore.Audio.Media.IS_RINGTONE} INTEGER NOT NULL,
                    ${MediaStore.Audio.Media.DATA} TEXT,
                    ${MediaStore.Audio.Media.SIZE} INTEGER,
                    ${MediaStore.Audio.Media.DISPLAY_NAME} TEXT,
                    ${MediaStore.Audio.Media.DATE_ADDED} INTEGER,
                    ${MediaStore.Audio.Media.DATE_MODIFIED} INTEGER
                );
            """.trimIndent()
        )

        db.execSQL(
            """
                CREATE TABLE $TABLE_ALBUM (
                    ${MediaStore.Audio.Albums._ID} INTEGER PRIMARY KEY,
                    ${MediaStore.Audio.Albums.ALBUM} TEXT,
                    ${MediaStore.Audio.Albums.ALBUM_KEY} TEXT,
                    ${MediaStore.Audio.Media.ARTIST_ID} INTEGER NOT NULL,
                    ${MediaStore.Audio.Albums.ARTIST} TEXT,
                    ${MediaStore.Audio.Albums.NUMBER_OF_SONGS} INTEGER,
                    ${MediaStore.Audio.Albums.FIRST_YEAR} INTEGER,
                    ${MediaStore.Audio.Albums.LAST_YEAR} INTEGER,
                    ${MediaStore.Audio.Albums.ALBUM_ART} TEXT
                );
            """.trimIndent()
        )

        db.execSQL(
            """
                CREATE TABLE $TABLE_ARTIST (
                    ${MediaStore.Audio.Artists._ID} INTEGER PRIMARY KEY,
                    ${MediaStore.Audio.Artists.ARTIST} TEXT,
                    ${MediaStore.Audio.Artists.ARTIST_KEY} TEXT,
                    ${MediaStore.Audio.Artists.NUMBER_OF_TRACKS} INTEGER,
                    ${MediaStore.Audio.Artists.NUMBER_OF_ALBUMS} INTEGER
                );
            """.trimIndent()
        )
    }

    private fun insertSampleMedia(db: SQLiteDatabase) {
        db.track(
            161,
            "1741 (The Battle of Cartagena)",
            65,
            "Sunset on the Golden Age",
            26,
            "Alestorm",
            437603,
            1004,
            "Music/1741_(The_Battle_of_Cartagena).mp3",
            1466283480,
            17_506_481
        )
        db.track(
            309,
            "The 2nd Law: Isolated System",
            40,
            "The 2nd Law",
            18,
            "Muse",
            300042,
            1013,
            "Music/The_2nd_Law_(Isolated_System).mp3",
            1439653800,
            12_075_967
        )
        db.track(
            865,
            "Algorithm",
            98,
            "Simulation Theory",
            18,
            "Muse",
            245960,
            1001,
            "Music/Simulation Theory/Algorithm.mp3",
            1576838717,
            10_806_478
        )
        db.track(
            481,
            "Dirty Water",
            102,
            "Concrete and Gold",
            13,
            "Foo Fighters",
            320914,
            1006,
            "Music/Concrete And Gold/Dirty_Water.mp3",
            1506374520,
            12_912_282
        )
        db.track(
            48,
            "Give It Up",
            7,
            "Greatest Hits 30 Anniversary Edition",
            5,
            "AC/DC",
            233592,
            1019,
            "Music/Give_It_Up.mp3",
            1455310080,
            5_716_578
        )
        db.track(
            125,
            "Jailbreak",
            7,
            "Greatest Hits 30 Anniversary Edition",
            5,
            "AC/DC",
            276668,
            2014,
            "Music/Jailbreak.mp3",
            1455310140,
            6_750_404
        )
        db.track(
            294,
            "Knights of Cydonia",
            38,
            "Black Holes and Revelations",
            18,
            "Muse",
            366946,
            1011,
            "Music/Knights_of_Cydonia.mp3",
            1414880700,
            11_746_572
        )
        db.track(
            219,
            "A Matter of Time",
            26,
            "Wasting Light",
            18,
            "Foo Fighters",
            276140,
            1008,
            "Music/Wasting Light/A_Matter_of_Time.mp3",
            1360677660,
            11_149_678
        )
        db.track(
            75,
            "Nightmare",
            6,
            "Nightmare",
            4,
            "Avenged Sevenfold",
            374648,
            1001,
            "Music/Nightmare.mp3",
            1439590380,
            10_828_662
        )
        db.track(
            464,
            "The Pretenders",
            95,
            "Echoes, Silence, Patience & Grace",
            13,
            "Foo Fighters",
            266509,
            1001,
            "Music/The_Pretenders.mp3",
            1439653740,
            4_296_041
        )
        db.track(
            477,
            "Run",
            102,
            "Concrete and Gold",
            13,
            "Foo Fighters",
            323424,
            1002,
            "Music/Concrete And Gold/Run.mp3",
            1506374520,
            13_012_576
        )
    }

    private fun insertSampleAlbums(db: SQLiteDatabase) {
        db.album(
            40,
            "The 2nd Law",
            18,
            "Muse",
            1,
            2012,
            "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627051019"
        )
        db.album(
            38,
            "Black Holes and Revelations",
            13,
            "Muse",
            1,
            2006,
            "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627043599"
        )
        db.album(
            102,
            "Concrete and Gold",
            13,
            "Foo Fighters",
            2,
            2017,
            "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627413029"
        )
        db.album(
            95,
            "Echoes, Silence, Patience & Grace",
            13,
            "Foo Fighters",
            1,
            2007,
            "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627124517"
        )
        db.album(7, "Greatest Hits Anniversary Edition", 5, "AC/DC", 2, 2010, null)
        db.album(
            6,
            "Nightmare",
            4,
            "Avenged Sevenfold",
            1,
            2010,
            "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509626949249"
        )
        db.album(98, "Simulation Theory", 18, "Muse", 1, 2018, null)
        db.album(
            65,
            "Sunset on the Golden Age",
            26,
            "Alestorm",
            1,
            2014,
            "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509626970548"
        )
        db.album(
            26,
            "Wasting Light",
            13,
            "Foo Fighters",
            1,
            2011,
            "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627009356"
        )
    }

    private fun insertSampleArtists(db: SQLiteDatabase) {
        db.artist(5, "AC/DC", 2, 1)
        db.artist(26, "Alestorm", 1, 1)
        db.artist(4, "Avenged Sevenfold", 1, 1)
        db.artist(13, "Foo Fighters", 4, 3)
        db.artist(18, "Muse", 2, 2)
    }

    private fun SQLiteDatabase.track(
        id: Long,
        title: String,
        albumId: Long,
        albumTitle: String,
        artistId: Long,
        artistName: String,
        duration: Long,
        trackNo: Int,
        filepath: String,
        dateAdded: Long,
        fileSize: Long
    ) = insert(TABLE_MEDIA, null, ContentValues(25).apply {
        put(MediaStore.Audio.Media._ID, id)
        put(MediaStore.Audio.Media.TITLE, title)
        put(MediaStore.Audio.Media.TITLE_KEY, MediaStore.Audio.keyFor(title))
        put(MediaStore.Audio.Media.ALBUM_ID, albumId)
        put(MediaStore.Audio.Media.ALBUM, albumTitle)
        put(MediaStore.Audio.Media.ALBUM_KEY, MediaStore.Audio.keyFor(albumTitle))
        put(MediaStore.Audio.Media.ARTIST_ID, artistId)
        put(MediaStore.Audio.Media.ARTIST, artistName)
        put(MediaStore.Audio.Media.ARTIST_KEY, MediaStore.Audio.keyFor(artistName))
        put(MediaStore.Audio.Media.DURATION, duration)
        put(MediaStore.Audio.Media.TRACK, trackNo)
        put(MediaStore.Audio.Media.BOOKMARK, 0)
        put(MediaStore.Audio.Media.IS_MUSIC, 1)
        put(MediaStore.Audio.Media.IS_ALARM, 0)
        put(MediaStore.Audio.Media.IS_NOTIFICATION, 0)
        put(MediaStore.Audio.Media.IS_PODCAST, 0)
        put(MediaStore.Audio.Media.IS_RINGTONE, 0)
        put(MediaStore.Audio.Media.DATA, filepath)
        put(MediaStore.Audio.Media.SIZE, fileSize)
        put(
            MediaStore.Audio.Media.DISPLAY_NAME,
            filepath.substringAfterLast(File.pathSeparatorChar)
        )
        put(MediaStore.Audio.Media.DATE_ADDED, dateAdded)
        put(MediaStore.Audio.Media.DATE_MODIFIED, dateAdded)
    })

    private fun SQLiteDatabase.album(
        id: Long,
        title: String,
        artistId: Long,
        artistName: String,
        numberOfSongs: Int,
        releaseYear: Int,
        albumArtPath: String?
    ) = insert(TABLE_ALBUM, null, ContentValues(10).apply {
        put(MediaStore.Audio.Albums._ID, id)
        put(MediaStore.Audio.Albums.ALBUM, title)
        put(MediaStore.Audio.Albums.ALBUM_KEY, MediaStore.Audio.keyFor(title))
        put(MediaStore.Audio.Media.ARTIST_ID, artistId)
        put(MediaStore.Audio.Albums.ARTIST, artistName)
        put(MediaStore.Audio.Albums.NUMBER_OF_SONGS, numberOfSongs)
        put(MediaStore.Audio.Albums.FIRST_YEAR, releaseYear)
        put(MediaStore.Audio.Albums.LAST_YEAR, releaseYear)
        put(MediaStore.Audio.Albums.ALBUM_ART, albumArtPath)
    })

    private fun SQLiteDatabase.artist(
        id: Long,
        name: String,
        numberOfTracks: Int,
        numberOfAlbums: Int
    ) = insert(TABLE_ARTIST, null, ContentValues(5).apply {
        put(MediaStore.Audio.Artists._ID, id)
        put(MediaStore.Audio.Artists.ARTIST, name)
        put(MediaStore.Audio.Artists.ARTIST_KEY, MediaStore.Audio.keyFor(name))
        put(MediaStore.Audio.Artists.NUMBER_OF_TRACKS, numberOfTracks)
        put(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS, numberOfAlbums)
    })
}
