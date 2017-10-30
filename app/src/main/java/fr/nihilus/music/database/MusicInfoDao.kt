package fr.nihilus.music.database

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.support.annotation.WorkerThread

@Dao
interface MusicInfoDao {

    @WorkerThread
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addStats(infos: Iterable<MusicInfo>)
}
