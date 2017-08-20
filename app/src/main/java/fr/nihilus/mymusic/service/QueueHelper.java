package fr.nihilus.mymusic.service;

import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import fr.nihilus.mymusic.utils.MediaID;

@Deprecated
final class QueueHelper {

    private static final String TAG = "QueueHelper";

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

    private static List<QueueItem> convertToQueue(Collection<MediaMetadataCompat> tracks,
                                                  String... categories) {
        List<QueueItem> queue = new ArrayList<>();
        int count = 0;
        for (MediaMetadataCompat track : tracks) {
            String hierarchyAwareMediaID = MediaID
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

    @Nullable
    static List<QueueItem> getPlayingQueue(String mediaId, MusicProvider provider) {
        String[] hierarchy = MediaID.getHierarchy(mediaId);

        if (hierarchy.length < 1) {
            Log.e(TAG, "getPlayingQueue: could not build playing queue for this mediaId: " + mediaId);
            return null;
        }

        String categoryType = hierarchy[0];
        String musicId = MediaID.extractMusicID(mediaId);

        List<MediaMetadataCompat> tracks = null;
        switch (categoryType) {
            case MediaID.ID_MUSIC:
                tracks = provider.getAllMusic();
                break;
            case MediaID.ID_ALBUMS:
                String albumId = hierarchy[1];
                tracks = provider.getAlbumTracks(albumId);
                break;
            case MediaID.ID_ARTISTS:
                String artistId = hierarchy[1];
                tracks = provider.getArtistTracks(artistId);
                break;
            case MediaID.ID_PLAYLISTS:
                String playlistId = hierarchy[1];
                tracks = provider.getPlaylistMembers(playlistId);
                break;
            case MediaID.ID_DAILY:
                MediaMetadataCompat daily = provider.getMusic(musicId);
                tracks = Collections.singletonList(daily);
                break;
        }

        if (tracks == null) {
            Log.e(TAG, "getPlayingQueue: unrecognized category type: "
                    + categoryType + " for mediaId " + mediaId);
            return null;
        }

        return convertToQueue(tracks, hierarchy);
    }

    static void shuffleQueue(List<QueueItem> queue) {
        Collections.shuffle(queue);
    }

    static void sortQueue(List<QueueItem> queue) {
        Collections.sort(queue, new Comparator<QueueItem>() {
            @Override
            public int compare(QueueItem one, QueueItem another) {
                return (int) (one.getQueueId() - another.getQueueId());
            }
        });
    }
}
