package fr.nihilus.mymusic.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.support.annotation.WorkerThread;

@Dao
public interface MusicInfoDao {

    @WorkerThread
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void addStats(Iterable<MusicInfo> infos);
}
