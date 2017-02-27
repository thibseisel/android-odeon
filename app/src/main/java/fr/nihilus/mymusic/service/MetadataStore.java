package fr.nihilus.mymusic.service;


import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.util.LongSparseArray;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

class MetadataStore {

    static final Comparator<MediaMetadataCompat> SORT_TRACKNO = new Comparator<MediaMetadataCompat>() {
        @Override
        public int compare(MediaMetadataCompat one, MediaMetadataCompat another) {
            long oneTrack = one.getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER) * 100
                    + one.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER);
            long anotherTrack = another.getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER) * 100
                    + another.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER);
            return (int) (oneTrack - anotherTrack);
        }
    };

    @SuppressWarnings("WrongConstant")
    static final Comparator<MediaMetadataCompat> SORT_TITLE = new Comparator<MediaMetadataCompat>() {
        @Override
        public int compare(MediaMetadataCompat one, MediaMetadataCompat another) {
            String oneKey = one.getString(MusicProvider.METADATA_TITLE_KEY);
            String anotherKey = another.getString(MusicProvider.METADATA_TITLE_KEY);
            if (oneKey != null && anotherKey != null) {
                return oneKey.compareTo(anotherKey);
            }
            return 0;
        }
    };

    private final LongSparseArray<SortedSet<MediaMetadataCompat>> mSparseArray;
    private final Comparator<MediaMetadataCompat> mSorting;

    MetadataStore(Comparator<MediaMetadataCompat> sorting) {
        mSparseArray = new LongSparseArray<>();
        mSorting = sorting;
    }

    void put(long id, MediaMetadataCompat metadata) {
        SortedSet<MediaMetadataCompat> set = mSparseArray.get(id);
        if (set == null) {
            set = new TreeSet<>(mSorting);
            mSparseArray.put(id, set);
        }
        set.add(metadata);
    }

    boolean isEmpty() {
        return mSparseArray.size() == 0;
    }

    @Nullable
    public SortedSet<MediaMetadataCompat> get(long id) {
        return mSparseArray.get(id, null);
    }

    public void clear() {
        mSparseArray.clear();
    }
}
