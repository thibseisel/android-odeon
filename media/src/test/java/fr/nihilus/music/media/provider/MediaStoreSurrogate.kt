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

package fr.nihilus.music.media.provider

import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.provider.BaseColumns
import android.provider.MediaStore.Audio.*
import fr.nihilus.music.media.os.ContentResolverDelegate
import java.io.File

private const val TABLE_MEDIA = "media"
private const val TABLE_ALBUM = "album"
private const val TABLE_ARTIST = "artist"

/**
 * A surrogate of the Android media provider backed by an in-memory SQLite Database.
 * It implements the same contract as [android.provider.MediaStore.Audio]
 * for media, albums and artists.
 *
 * @param context The application context, needed for creating the database.
 */
internal class MediaStoreSurrogate(
    context: Context
) : ContentResolverDelegate {

    private val inMemoryDatabaseHelper = InMemoryMediaStoreDatabase(context)
    internal val observers = mutableSetOf<ObserverSpec>()

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val tableName = requireNotNull(getTableNameFor(uri)) { "Unsupported Uri: $uri" }
        val db = inMemoryDatabaseHelper.readableDatabase
        return db.query(tableName, projection, selection, selectionArgs, null, null, sortOrder)
    }

    private fun getTableNameFor(uri: Uri): String? = when (uri) {
        Media.EXTERNAL_CONTENT_URI -> TABLE_MEDIA
        Albums.EXTERNAL_CONTENT_URI -> TABLE_ALBUM
        Artists.EXTERNAL_CONTENT_URI -> TABLE_ARTIST
        else -> null
    }

    override fun delete(uri: Uri, where: String?, whereArgs: Array<String>?): Int {
        val tableName = requireNotNull(getTableNameFor(uri)) { "Unsupported Uri: $uri" }
        return inMemoryDatabaseHelper.writableDatabase.delete(tableName, where, whereArgs)
    }

    override fun registerContentObserver(
        uri: Uri,
        notifyForDescendants: Boolean,
        observer: ContentObserver
    ) {
        val spec = ObserverSpec(uri, notifyForDescendants, observer)
        observers += spec
    }

    override fun unregisterContentObserver(observer: ContentObserver) {
        observers.removeAll { it.observer == observer }
    }

    /**
     * Check that a row exists for the given [media type][type] and having the specified [id].
     * @return `true` if one row exists, `false` otherwise.
     */
    fun exist(type: MediaProvider.MediaType, id: Long): Boolean {
        val tableName = when (type) {
            MediaProvider.MediaType.TRACKS -> TABLE_MEDIA
            MediaProvider.MediaType.ALBUMS -> TABLE_ALBUM
            MediaProvider.MediaType.ARTISTS -> TABLE_ARTIST
        }

        val db = inMemoryDatabaseHelper.readableDatabase
        return db.query(
            tableName,
            arrayOf(BaseColumns._ID),
            "${BaseColumns._ID} = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )?.count == 1
    }

    /**
     * Release resources allocated by the database.
     */
    fun release() {
        inMemoryDatabaseHelper.close()
    }

    internal data class ObserverSpec(
        val uri: Uri,
        val notifyForDescendants: Boolean,
        val observer: ContentObserver
    )
}

private class InMemoryMediaStoreDatabase(context: Context) : SQLiteOpenHelper(context, null, null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        createSchema(db)
        insertSampleMedia(db)
        insertSampleAlbums(db)
        insertSampleArtists(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Nothing to do. In-memory database will be recreated with the correct schema.
    }

    private fun createSchema(db: SQLiteDatabase) {
        db.execSQL("""
                CREATE TABLE $TABLE_MEDIA (
                    ${Media._ID} INTEGER PRIMARY KEY,
                    ${Media.TITLE} TEXT,
                    ${Media.TITLE_KEY} TEXT,
                    ${Media.ALBUM_ID} INTEGER NOT NULL,
                    ${Media.ALBUM} TEXT,
                    ${Media.ALBUM_KEY} TEXT,
                    ${Media.ARTIST_ID} INTEGER NOT NULL,
                    ${Media.ARTIST} TEXT,
                    ${Media.ARTIST_KEY} TEXT,
                    ${Media.DURATION} INTEGER,
                    ${Media.TRACK} INTEGER,
                    ${Media.COMPOSER} TEXT,
                    ${Media.BOOKMARK} INTEGER,
                    ${Media.IS_MUSIC} INTEGER NOT NULL,
                    ${Media.IS_ALARM} INTEGER NOT NULL,
                    ${Media.IS_NOTIFICATION} INTEGER NOT NULL,
                    ${Media.IS_PODCAST} INTEGER NOT NULL,
                    ${Media.IS_RINGTONE} INTEGER NOT NULL,
                    ${Media.DATA} TEXT,
                    ${Media.SIZE} INTEGER,
                    ${Media.DISPLAY_NAME} TEXT,
                    ${Media.DATE_ADDED} INTEGER,
                    ${Media.DATE_MODIFIED} INTEGER
                );
            """.trimIndent())

        db.execSQL("""
                CREATE TABLE $TABLE_ALBUM (
                    ${Albums._ID} INTEGER PRIMARY KEY,
                    ${Albums.ALBUM} TEXT,
                    ${Albums.ALBUM_KEY} TEXT,
                    ${Media.ARTIST_ID} INTEGER NOT NULL,
                    ${Albums.ARTIST} TEXT,
                    ${Albums.NUMBER_OF_SONGS} INTEGER,
                    ${Albums.FIRST_YEAR} INTEGER,
                    ${Albums.LAST_YEAR} INTEGER,
                    ${Albums.ALBUM_ART} TEXT
                );
            """.trimIndent())

        db.execSQL("""
                CREATE TABLE $TABLE_ARTIST (
                    ${Artists._ID} INTEGER PRIMARY KEY,
                    ${Artists.ARTIST} TEXT,
                    ${Artists.ARTIST_KEY} TEXT,
                    ${Artists.NUMBER_OF_TRACKS} INTEGER,
                    ${Artists.NUMBER_OF_ALBUMS} INTEGER
                );
            """.trimIndent())
    }

    private fun insertSampleMedia(db: SQLiteDatabase) {
        db.track(161, "1741 (The Battle of Cartagena)", 65, "Sunset on the Golden Age", 26, "Alestorm", 437603, 1004, "Music/1741_(The_Battle_of_Cartagena).mp3", 1466283480)
        db.track(309, "The 2nd Law: Isolated System", 40, "The 2nd Law", 18, "Muse", 300042, 1013, "Music/The_2nd_Law_(Isolated_System).mp3", 1439653800)
        db.track(481, "Dirty Water", 102, "Concrete and Gold", 13, "Foo Fighters", 320914, 1006, "Music/Concrete And Gold/Dirty_Water.mp3", 1506374520)
        db.track(48, "Give It Up", 7, "Greatest Hits 30 Anniversary Edition", 5, "AC/DC", 233592, 1019, "Music/Give_It_Up.mp3", 1455310080)
        db.track(125, "Jailbreak", 7, "Greatest Hits 30 Anniversary Edition", 5, "AC/DC", 276668, 2014, "Music/Jailbreak.mp3", 1455310140)
        db.track(294, "Knights of Cydonia", 38, "Black Holes and Revelations", 18, "Muse", 366946, 1011, "Music/Knights_of_Cydonia.mp3", 1414880700)
        db.track(219, "A Matter of Time", 26, "Wasting Light", 18, "Foo Fighters", 276140, 1008, "Music/Wasting Light/A_Matter_of_Time.mp3", 1360677660)
        db.track(75, "Nightmare", 6, "Nightmare", 4, "Avenged Sevenfold", 374648, 1001, "Music/Nightmare.mp3", 1439590380)
        db.track(464, "The Pretenders", 95, "Echoes, Silence, Patience & Grace", 13, "Foo Fighters", 266509, 1001, "Music/The_Pretenders.mp3", 1439653740)
        db.track(477, "Run", 102, "Concrete and Gold", 13, "Foo Fighters", 323424, 1002, "Music/Concrete And Gold/Run.mp3", 1506374520)
    }

    private fun insertSampleAlbums(db: SQLiteDatabase) {
        db.album(40, "The 2nd Law", 18, "Muse", 1, 2012, "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627051019")
        db.album(38, "Black Holes and Revelations", 13, "Muse", 1, 2006, "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627043599")
        db.album(102, "Concrete and Gold", 13, "Foo Fighters", 2, 2017, "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627413029")
        db.album(95, "Echoes, Silence, Patience & Grace", 13, "Foo Fighters", 1, 2007, "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627124517")
        db.album(7, "Greatest Hits Anniversary Edition", 5, "AC/DC", 2,2010, null)
        db.album(6, "Nightmare", 4, "Avenged Sevenfold", 1, 2010, "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509626949249")
        db.album(65, "Sunset on the Golden Age", 26, "Alestorm", 1, 2014, "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509626970548")
        db.album(26, "Wasting Light", 13, "Foo Fighters", 1, 2011, "/storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1509627009356")
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
        dateAdded: Long
    ) = insert(TABLE_MEDIA, null, ContentValues(25).apply {
        put(Media._ID, id)
        put(Media.TITLE, title)
        put(Media.TITLE_KEY, keyFor(title))
        put(Media.ALBUM_ID, albumId)
        put(Media.ALBUM, albumTitle)
        put(Media.ALBUM_KEY, keyFor(albumTitle))
        put(Media.ARTIST_ID, artistId)
        put(Media.ARTIST, artistName)
        put(Media.ARTIST_KEY, keyFor(artistName))
        put(Media.DURATION, duration)
        put(Media.TRACK, trackNo)
        put(Media.BOOKMARK, 0)
        put(Media.IS_MUSIC, 1)
        put(Media.IS_ALARM, 0)
        put(Media.IS_NOTIFICATION, 0)
        put(Media.IS_PODCAST, 0)
        put(Media.IS_RINGTONE, 0)
        put(Media.DATA, filepath)
        put(Media.SIZE, 0)
        put(Media.DISPLAY_NAME, filepath.substringAfterLast(File.pathSeparatorChar))
        put(Media.DATE_ADDED, dateAdded)
        put(Media.DATE_MODIFIED, dateAdded)
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
        put(Albums._ID, id)
        put(Albums.ALBUM, title)
        put(Albums.ALBUM_KEY, keyFor(title))
        put(Media.ARTIST_ID, artistId)
        put(Albums.ARTIST, artistName)
        put(Albums.NUMBER_OF_SONGS, numberOfSongs)
        put(Albums.FIRST_YEAR, releaseYear)
        put(Albums.LAST_YEAR, releaseYear)
        put(Albums.ALBUM_ART, albumArtPath)
    })

    private fun SQLiteDatabase.artist(
        id: Long,
        name: String,
        numberOfTracks: Int,
        numberOfAlbums: Int
    ) = insert(TABLE_ARTIST, null, ContentValues(5).apply {
        put(Artists._ID, id)
        put(Artists.ARTIST, name)
        put(Artists.ARTIST_KEY, keyFor(name))
        put(Artists.NUMBER_OF_TRACKS, numberOfTracks)
        put(Artists.NUMBER_OF_ALBUMS, numberOfAlbums)
    })
}
