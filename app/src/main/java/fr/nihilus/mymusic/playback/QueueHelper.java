package fr.nihilus.mymusic.playback;

import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.util.LongSparseArray;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import fr.nihilus.mymusic.utils.MediaIDHelper;

final class QueueHelper {

    private static final String TAG = "QueueHelper";

    static List<QueueItem> getAllMusic(MusicProvider provider) {
        LongSparseArray<MediaMetadataCompat> tracks = provider.getAllMusic();
        ArrayList<QueueItem> queue = new ArrayList<>(tracks.size());
        int index = 0;
        for (int i = 0; i < tracks.size(); i++) {
            final MediaMetadataCompat track = tracks.valueAt(i);
            QueueItem item = new QueueItem(track.getDescription(), index++);
            queue.add(item);
        }
        sortQueueAlpha(queue);
        return queue;
    }

    /**
     * Indique si l'index donné est bien compris dans la file.
     */
    static boolean isIndexPlayable(int index, List<QueueItem> queue) {
        return (queue != null && index >= 0 && index < queue.size());
    }

    /**
     * Retrouve la position de l'item portant l'id donné dans la file.
     */
    static int getMusicIndexOnQueue(Iterable<QueueItem> queue, long queueId) {
        int index = 0;
        for (QueueItem item : queue) {
            if (queueId == item.getQueueId()) {
                return index;
            }
            index++;
        }
        return -1;
    }

    static int getMusicIndexOnQueue(List<QueueItem> queue, String mediaId) {
        int index = 0;
        for (QueueItem item : queue) {
            if (mediaId.equals(item.getDescription().getMediaId())) {
                return index;
            }
            index++;
        }
        return -1;
    }

    static List<QueueItem> convertToQueue(Collection<MediaMetadataCompat> tracks,
                                          String... categories) {
        List<QueueItem> queue = new ArrayList<>();
        int count = 0;
        for (MediaMetadataCompat track : tracks) {
            String hierarchyAwareMediaID = MediaIDHelper
                    .createMediaID(track.getDescription().getMediaId(), categories);

            MediaMetadataCompat trackCopy = new MediaMetadataCompat.Builder(track)
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, hierarchyAwareMediaID)
                    .build();

            // Comme la queue ne change pas, on utilise l'index comme id
            QueueItem item = new QueueItem(trackCopy.getDescription(), count++);
            queue.add(item);
        }
        return queue;
    }

    static List<QueueItem> getPlayingQueue(String mediaId, MusicProvider provider) {
        String[] hierarchy = MediaIDHelper.getHierarchy(mediaId);

        if (hierarchy.length != 2) {
            Log.e(TAG, "getPlayingQueue: could not build playing queue for this mediaId: " + mediaId);
            return null;
        }

        String categoryType = hierarchy[0];
        String categoryValue = hierarchy[1];
        Log.d(TAG, "getPlayingQueue: creating playing queue for " + categoryType
                + ", " + categoryValue);

        Collection<MediaMetadataCompat> tracks = null;
        if (categoryType.equals(MediaIDHelper.MEDIA_ID_MUSIC)) {
            // FIXME Eviter d'avoir à copier depuis LongSparseArray
            LongSparseArray<MediaMetadataCompat> allMusic = provider.getAllMusic();
            tracks = new ArrayList<>(allMusic.size());
            for (int i = 0; i < allMusic.size(); i++) {
                tracks.add(allMusic.valueAt(i));
            }
        }
        // TODO Gérer les autres cas (par albums, recherche...)

        if (tracks == null) {
            Log.e(TAG, "getPlayingQueue: unrecognized category type: "
                    + categoryType + " for mediaId " + mediaId);
            return null;
        }

        return convertToQueue(tracks, hierarchy[0], hierarchy[1]);
    }

    public static void shuffleQueue(List<QueueItem> queue) {
        Collections.shuffle(queue);
    }

    public static void sortQueue(List<QueueItem> queue) {
        Collections.sort(queue, new Comparator<QueueItem>() {
            @Override
            public int compare(QueueItem one, QueueItem another) {
                return Long.compare(one.getQueueId(), another.getQueueId());
            }
        });
    }

    private static void sortQueueAlpha(List<QueueItem> queue) {
        Collections.sort(queue, new Comparator<QueueItem>() {
            @Override
            public int compare(QueueItem one, QueueItem another) {
                String oneTitle = one.getDescription().getTitle().toString();
                String anotherTitle = another.getDescription().getTitle().toString();
                return oneTitle.compareTo(anotherTitle);
            }
        });
    }
}
