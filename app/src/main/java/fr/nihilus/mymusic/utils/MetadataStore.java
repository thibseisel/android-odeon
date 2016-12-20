package fr.nihilus.mymusic.utils;


import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.util.LongSparseArray;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

public class MetadataStore {

    private final LongSparseArray<SortedSet<MediaMetadataCompat>> mSparseArray;

    public MetadataStore() {
        mSparseArray = new LongSparseArray<>();
    }

    public void put(long id, MediaMetadataCompat metadata) {
        SortedSet<MediaMetadataCompat> set = mSparseArray.get(id);
        if (set == null) {
            set = new TreeSet<>(SORT_TRACKNO);
            mSparseArray.put(id, set);
        }
        set.add(metadata);
    }

    @Nullable
    public SortedSet<MediaMetadataCompat> get(long id) {
        return mSparseArray.get(id, null);
    }

    public void clear() {
        mSparseArray.clear();
    }

    private static final Comparator<MediaMetadataCompat> SORT_TRACKNO = new Comparator<MediaMetadataCompat>() {
        @Override
        public int compare(MediaMetadataCompat one, MediaMetadataCompat another) {
            long oneTrack = one.getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER) * 100
                    + one.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER);
            long anotherTrack = another.getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER) * 100
                    + another.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER);
            return (int) (oneTrack - anotherTrack);
        }
    };
}
