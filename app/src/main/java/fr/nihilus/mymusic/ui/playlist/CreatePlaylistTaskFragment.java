package fr.nihilus.mymusic.ui.playlist;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.util.Log;

import fr.nihilus.mymusic.provider.Playlists;
import fr.nihilus.mymusic.utils.MediaID;

@Deprecated
public class CreatePlaylistTaskFragment extends Fragment {

    static final String TAG = "CreatePlaylistTask";

    private static final String ARG_TITLE = "title";
    private static final String ARG_SONGS = "songs";

    private CharSequence mTitle;
    private MediaItem[] mSongs;

    public static CreatePlaylistTaskFragment newInstance(CharSequence playlistTitle, MediaItem[] songs) {
        Bundle args = new Bundle();
        args.putCharSequence(ARG_TITLE, playlistTitle);
        args.putParcelableArray(ARG_SONGS, songs);
        CreatePlaylistTaskFragment fragment = new CreatePlaylistTaskFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        Bundle args = getArguments();
        if (args == null) {
            throw new IllegalStateException();
        }

        mTitle = args.getCharSequence(ARG_TITLE);
        mSongs = (MediaItem[]) args.getParcelableArray(ARG_SONGS);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        new AsyncTask<MediaItem, Void, Integer>() {
            @Override
            protected Integer doInBackground(MediaItem... params) {
                final ContentResolver resolver = getContext().getApplicationContext().getContentResolver();

                // Insert playlist
                ContentValues playlistValues = new ContentValues(2);
                playlistValues.put(Playlists.NAME, mTitle.toString());
                playlistValues.put(Playlists.DATE_CREATED, System.currentTimeMillis());
                Uri playlistUri = resolver.insert(Playlists.CONTENT_URI, playlistValues);
                long playlistId = ContentUris.parseId(playlistUri);

                Log.v(TAG, "doInBackground: created playlist at URI: " + playlistUri);
                Log.v(TAG, "doInBackground: must insert " + params.length + " songs");

                // Insert songs
                ContentValues[] values = new ContentValues[params.length];
                for (int i = 0; i < params.length; i++) {
                    long songId = Long.parseLong(MediaID.extractMusicID(params[i].getMediaId()));
                    values[i] = new ContentValues(2);
                    values[i].put(Playlists.Members.MUSIC, songId);
                    values[i].put(Playlists.Members.POSITION, i);
                }

                return resolver.bulkInsert(Playlists.Members.getContentUri(playlistId), values);
            }

            @Override
            protected void onPostExecute(Integer insertedSongs) {
                Log.d(TAG, "onPostExecute: inserted " + insertedSongs + " songs into playlist.");
                Fragment target = getTargetFragment();
                if (target instanceof NewPlaylistFragment) {
                    ((NewPlaylistFragment) target).dismiss();
                }

                // Removes itself from the FragmentManager when done
                getFragmentManager().beginTransaction().remove(CreatePlaylistTaskFragment.this).commit();
            }
        }.execute(mSongs);
    }
}
