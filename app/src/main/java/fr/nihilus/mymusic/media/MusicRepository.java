package fr.nihilus.mymusic.media;

import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat.MediaItem;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.nihilus.mymusic.database.PlaylistDao;
import fr.nihilus.mymusic.utils.MediaID;

@Singleton
public class MusicRepository {
    private final MediaDao mMediaDao;
    private final PlaylistDao mPlaylistDao;

    @Inject
    MusicRepository(@NonNull MediaDao mediaDao, @NonNull PlaylistDao playlistDao) {
        mMediaDao = mediaDao;
        mPlaylistDao = playlistDao;
    }

    public void init() {

    }

    public List<MediaItem> getMediaItems(@NonNull String parentMediaId) {
        switch (parentMediaId) {
            case MediaID.ID_MUSIC:
                return null;
        }
        return Collections.emptyList();
    }
}
