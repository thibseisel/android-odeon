/*
 * Copyright 2018 Thibault Seisel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.nihilus.music.ui.playlist;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import dagger.android.support.AndroidSupportInjection;
import fr.nihilus.music.R;
import fr.nihilus.music.client.BrowserViewModel;
import fr.nihilus.music.command.MediaSessionCommand;
import fr.nihilus.music.command.NewPlaylistCommand;
import fr.nihilus.music.di.ActivityScoped;
import fr.nihilus.music.ui.songs.SongAdapter;
import fr.nihilus.music.utils.MediaID;
import kotlin.Unit;

@ActivityScoped
public class NewPlaylistFragment extends AppCompatDialogFragment
        implements AdapterView.OnItemClickListener, View.OnClickListener {

    private static final String TAG = "NewPlaylistFragment";
    private static final String ARG_SONGS_IDS = "song_ids";

    private TextInputLayout titleLayout;
    private TextInputEditText titleInput;
    private ListView listView;
    private TextView message;
    private Button validateButton;

    private SongAdapter adapter;

    private BrowserViewModel viewModel;

    private final SubscriptionCallback subscriptionCallback = new SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaItem> children) {
            adapter.updateItems(children);

            // In case we have provided song ids as arguments
            Bundle args = getArguments();
            long[] songIds;
            if (args != null && (songIds = args.getLongArray(ARG_SONGS_IDS)) != null) {
                Arrays.sort(songIds);

                // Each song having the id of the ones specified as an argument is marked as selected
                int size = children.size();
                for (int i = 0; i < size; i++) {
                    String mediaId = children.get(i).getMediaId();
                    long musicId = Long.parseLong(MediaID.extractMusicID(mediaId));
                    int index = Arrays.binarySearch(songIds, musicId);
                    if (index >= 0) {
                        listView.setItemChecked(i, true);
                    }
                }

                updateCheckCountMessage(listView.getCheckedItemCount());
            }
        }
    };

    /**
     * Create a new instance of a dialog that allow to choose songs to add to a new playlist.
     *
     * @param songIds optional ids of songs to select for addition in the new playlist.
     * @return new fragment instance
     */
    public static NewPlaylistFragment newInstance(@Nullable long... songIds) {
        Bundle args = new Bundle();
        args.putLongArray(ARG_SONGS_IDS, songIds);
        NewPlaylistFragment fragment = new NewPlaylistFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new SongAdapter(this);
        setStyle(AppCompatDialogFragment.STYLE_NO_TITLE, R.style.AppTheme_DialogWhenLarge);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        viewModel = ViewModelProviders.of(getActivity()).get(BrowserViewModel.class);
    }

    @Override
    public void onStart() {
        super.onStart();
        viewModel.subscribe(MediaID.ID_MUSIC, subscriptionCallback);
    }

    @Override
    public void onStop() {
        viewModel.unsubscribe(MediaID.ID_MUSIC);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        titleLayout = null;
        titleInput = null;
        listView = null;
        message = null;
        validateButton = null;
        super.onDestroyView();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_playlist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        titleLayout = view.findViewById(R.id.titleLayout);
        titleInput = titleLayout.findViewById(R.id.title);

        validateButton = view.findViewById(R.id.validate);
        validateButton.setOnClickListener(this);

        message = view.findViewById(R.id.selected_songs);
        message.setText(R.string.new_playlist_help_message);

        listView = view.findViewById(android.R.id.list);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setOnItemClickListener(this);

        // Add a dismiss button to the toolbar
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_dialog);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_close) dismiss();
            return true;
        });
    }

    @Override
    public void onClick(View v) {
        final CharSequence playlistTitle = titleInput.getText();
        if (playlistTitle.length() == 0) {
            titleLayout.setError(getString(R.string.playlist_title_error));
            titleInput.requestFocus();
            return;
        }

        int checkedItemCount = listView.getCheckedItemCount();
        if (checkedItemCount == 0) {
            message.setText(R.string.new_playlist_help_message);
            return;
        }

        titleLayout.setError(null);

        SparseBooleanArray selectedSongs = listView.getCheckedItemPositions();
        Log.d(TAG, "onClick: selected items = " + selectedSongs.toString());

        long[] trackIds = new long[checkedItemCount];
        for (int index = 0, position = 0; index < selectedSongs.size(); index++) {
            if (selectedSongs.valueAt(index)) {
                MediaItem item = adapter.getItem(selectedSongs.keyAt(index));
                trackIds[position++] = Long.parseLong(MediaID.extractMusicID(item.getMediaId()));
            }
        }

        message.setText(R.string.saving_playlist);

        Bundle params = new Bundle(2);
        params.putString(NewPlaylistCommand.PARAM_TITLE, playlistTitle.toString());
        params.putLongArray(NewPlaylistCommand.PARAM_TRACK_IDS, trackIds);

        viewModel.post(controller -> {
            controller.sendCommand(NewPlaylistCommand.CMD_NAME, params, new ResultReceiver(new Handler()) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    switch (resultCode) {
                        case MediaSessionCommand.CODE_SUCCESS:
                            NewPlaylistFragment.this.dismiss();
                            break;
                        case NewPlaylistCommand.CODE_ERROR_TITLE_ALREADY_EXISTS:
                            message.setText(R.string.error_playlist_already_exists);
                            break;
                        default:
                            Log.e(TAG, "Unhandled result code: " + resultCode);
                    }
                }
            });

            return Unit.INSTANCE;
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        updateCheckCountMessage(listView.getCheckedItemCount());
    }

    private void updateCheckCountMessage(int checkedItemCount) {
        CharSequence selectedMessage = getResources()
                .getQuantityString(R.plurals.selected_song_count, checkedItemCount, checkedItemCount);
        message.setText(selectedMessage);
    }

}
