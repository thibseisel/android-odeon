package fr.nihilus.music.playlists;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.nihilus.music.database.Playlist;
import fr.nihilus.music.database.PlaylistDao;
import fr.nihilus.music.database.PlaylistTrack;
import fr.nihilus.music.database.PlaylistWithTracks;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.functions.Action;

@Singleton
public class PlaylistRepository {
    private final PlaylistDao mDao;

    @Inject
    PlaylistRepository(@NonNull PlaylistDao dao) {
        mDao = dao;
    }

    public Flowable<List<PlaylistWithTracks>> getPlaylists() {
        return mDao.getPlaylistsWithTracks();
    }

    public Completable saveNewPlaylist(final Playlist playlist, final long[] musicIds) {
        return Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
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

    public Completable addTracks(final List<PlaylistTrack> tracks) {
        return Completable.fromAction(new Action() {
            @Override
            public void run() {
                mDao.addTracks(tracks);
            }
        });
    }

    public Completable deletePlaylist(final long id) {
        return Completable.fromAction(new Action() {
            @Override
            public void run() {
                mDao.deletePlaylist(id);
            }
        });
    }

    public Completable deleteTracks(final PlaylistTrack[] tracks) {
        return Completable.fromAction(new Action() {
            @Override
            public void run() {
                mDao.deleteTracks(tracks);
            }
        });
    }


}
