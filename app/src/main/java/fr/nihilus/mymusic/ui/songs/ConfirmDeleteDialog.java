package fr.nihilus.mymusic.ui.songs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

import fr.nihilus.mymusic.R;
import fr.nihilus.mymusic.utils.MediaID;
import fr.nihilus.mymusic.utils.StringJoiner;

/**
 * A simple dialog that asks the user consent to delete songs from the music library.
 * The action of deleting is also handled by this dialog.
 */
public class ConfirmDeleteDialog extends DialogFragment implements DialogInterface.OnClickListener {
    static final String TAG = "ConfirmDeleteDialog";
    private static final String KEY_ITEMS = "itemsToDelete";

    private MediaBrowserCompat.MediaItem[] mToDelete;

    /**
     * Create a new instance of this DialogFragment with the specified items to delete.
     *
     * @param toDelete items to delete if the user presses the "Delete" button
     * @return new instance of this class
     */
    public static ConfirmDeleteDialog newInstance(MediaBrowserCompat.MediaItem[] toDelete) {
        Bundle args = new Bundle();
        args.putParcelableArray(KEY_ITEMS, toDelete);
        ConfirmDeleteDialog fragment = new ConfirmDeleteDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);

        Bundle args = getArguments();
        if (args != null) {
            mToDelete = (MediaBrowserCompat.MediaItem[]) args.getParcelableArray(KEY_ITEMS);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        String message = getResources().getQuantityString(R.plurals.delete_dialog_message,
                mToDelete.length, mToDelete.length);
        builder.setTitle(R.string.delete_dialog_title)
                .setMessage(message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.action_delete, this);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            new DeleteSongTask(getContext()).execute(mToDelete);
        }
    }

    /**
     * An {@link AsyncTask} that deletes selected songs from the device.
     */
    private static class DeleteSongTask extends AsyncTask<MediaBrowserCompat.MediaItem, Void, Integer> {

        private final Context mmContext;

        DeleteSongTask(Context context) {
            mmContext = context;
        }

        @Override
        protected Integer doInBackground(MediaBrowserCompat.MediaItem... params) {
            if (params == null || params.length == 0) {
                Log.e(TAG, "doInBackground: passed null or empty array. Cannot delete.");
                return 0;
            }

            StringJoiner inClause = new StringJoiner(",", " IN (", ")");
            for (MediaBrowserCompat.MediaItem item : params) {

                String filePath = item.getDescription().getExtras()
                        .getString(MediaStore.Audio.AudioColumns.DATA);
                File file = new File(filePath);
                if (file.delete()) {
                    String musicId = MediaID.extractMusicID(item.getMediaId());
                    inClause.add(musicId);
                }
            }

            String whereClause = BaseColumns._ID + inClause.toString();
            return mmContext.getContentResolver()
                    .delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, whereClause, null);
        }

        @Override
        protected void onPostExecute(Integer deleteCount) {
            String message = mmContext.getResources().getQuantityString(
                    R.plurals.deleted_songs_confirmation, deleteCount, deleteCount);
            Toast.makeText(mmContext, message, Toast.LENGTH_LONG).show();
        }
    }
}
