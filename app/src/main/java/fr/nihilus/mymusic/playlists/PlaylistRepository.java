package fr.nihilus.mymusic.playlists;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.nihilus.mymusic.AppExecutors;
import fr.nihilus.mymusic.database.Playlist;
import fr.nihilus.mymusic.database.PlaylistDao;
import fr.nihilus.mymusic.database.PlaylistTrack;
import fr.nihilus.mymusic.database.PlaylistWithTracks;
import io.reactivex.Flowable;

@Singleton
public class PlaylistRepository {
    private final PlaylistDao mDao;
    private final Executor mDiskExecutor;

    @Inject
    public PlaylistRepository(@NonNull PlaylistDao dao, @NonNull AppExecutors executors) {
        mDao = dao;
        mDiskExecutor = executors.diskIo();
    }

    public Flowable<List<PlaylistWithTracks>> getPlaylists() {
        return mDao.getPlaylistsWithTracks();
    }

    public void saveNewPlaylist(final Playlist playlist, final long[] musicIds) {
        mDiskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Long playlistId = mDao.savePlaylist(playlist);

                List<PlaylistTrack> playlistTracks = new ArrayList<>(musicIds.length);
                for (int index = 0; index < musicIds.length; index++) {
                    long musicId = musicIds[index];
                    PlaylistTrack track = new PlaylistTrack(playlistId, musicId);
                    track.setPosition(index);
                    playlistTracks.add(track);
                }

                mDao.addTracks(playlistTracks);
            }
        });
    }

    public void addTracks(final List<PlaylistTrack> tracks) {
        mDiskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mDao.addTracks(tracks);
            }
        });
    }

    public void deletePlaylist(final Playlist playlist) {
        mDiskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mDao.deletePlaylists(playlist);
            }
        });
    }

    public void deleteTracks(final PlaylistTrack[] tracks) {
        mDiskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mDao.deleteTracks(tracks);
            }
        });
    }


}
