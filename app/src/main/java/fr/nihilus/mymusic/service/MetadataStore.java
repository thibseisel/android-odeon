package fr.nihilus.mymusic.service;


import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.util.LongSparseArray;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

class MetadataStore {

    public static final int SORT_TYPE_TRACKNO = 0;
    public static final int SORT_TYPE_TITLE = 1;

    private final LongSparseArray<SortedSet<MediaMetadataCompat>> mSparseArray;
    private final int mSortType;

    MetadataStore(int sortType) {
        mSparseArray = new LongSparseArray<>();
        mSortType = sortType;
    }

    void put(long id, MediaMetadataCompat metadata) {
        SortedSet<MediaMetadataCompat> set = mSparseArray.get(id);
        if (set == null) {
            final Comparator<MediaMetadataCompat> comparator;
            if (mSortType == SORT_TYPE_TRACKNO) comparator = SORT_TRACKNO;
            else comparator = SORT_TITLE;

            set = new TreeSet<>(comparator);
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

    @SuppressWarnings("WrongConstant")
    private static final Comparator<MediaMetadataCompat> SORT_TITLE = new Comparator<MediaMetadataCompat>() {
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
}
