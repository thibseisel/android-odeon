package fr.nihilus.mymusic.provider;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.util.LongSparseArray;

import javax.inject.Inject;

import fr.nihilus.mymusic.di.MusicServiceScope;

@Deprecated
@MusicServiceScope
public class StatsRecorder {

    private final LongSparseArray<Integer> mSkips;
    private final LongSparseArray<Integer> mCompletions;

    @Inject
    public StatsRecorder() {
        mSkips = new LongSparseArray<>();
        mCompletions = new LongSparseArray<>();
    }

    public void recordCompletion(long musicId) {
        int current = mCompletions.get(musicId, 0);
        mCompletions.put(musicId, current + 1);
    }

    public void recordSkip(long musicId) {
        int current = mSkips.get(musicId, 0);
        mSkips.put(musicId, current + 1);
    }

    /**
     * Write statistics recorded by this class directly onto the storage.
     * Since it may take time to run, this method may be called preferably
     * when music service is not busy (for example, before it stops) or before querying statistics.
     * @param context current package context to access storage
     */
    public void publish(Context context) {
        final ContentResolver resolver = context.getContentResolver();
        Bundle args = new Bundle(3);

        // Incrementing the READ_COUNT field
        args.putString(DatabaseProvider.KEY_FIELD, Stats.READ_COUNT);
        long[] ids = new long[mCompletions.size()];
        int[] amounts = new int[mCompletions.size()];
        for (int i = 0; i < mCompletions.size(); i++) {
            ids[i] = mCompletions.keyAt(i);
            amounts[i] = mCompletions.valueAt(i);
        }
        args.putLongArray(DatabaseProvider.KEY_MUSIC_ID, ids);
        args.putIntArray(DatabaseProvider.KEY_AMOUNT, amounts);
        resolver.call(Stats.CONTENT_URI, DatabaseProvider.METHOD_BULK_INCREMENT, null, args);

        // Incrementing the SKIP_COUNT field
        args.putString(DatabaseProvider.KEY_FIELD, Stats.SKIP_COUNT);
        ids = new long[mSkips.size()];
        amounts = new int[mSkips.size()];
        for (int i = 0; i < mSkips.size(); i++) {
            ids[i] = mSkips.keyAt(i);
            amounts[i] = mSkips.valueAt(i);
        }
        args.putLongArray(DatabaseProvider.KEY_MUSIC_ID, ids);
        args.putIntArray(DatabaseProvider.KEY_AMOUNT, amounts);
        resolver.call(Stats.CONTENT_URI, DatabaseProvider.METHOD_BULK_INCREMENT, null, args);

        // Clear cached values
        mCompletions.clear();
        mSkips.clear();
    }
}
