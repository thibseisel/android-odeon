/*
 * Copyright 2017 Thibault Seisel
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

package fr.nihilus.music.ui.albums;

import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import fr.nihilus.music.R;
import fr.nihilus.music.media.MediaItems;
import fr.nihilus.music.utils.MediaID;
import fr.nihilus.music.utils.MediaItemDiffCallback;

class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackHolder> {

    private final List<MediaItem> mTracks = new ArrayList<>();
    private OnTrackSelectedListener mListener;

    @Override
    public TrackHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.album_track_item, parent, false);

        TrackHolder holder = new TrackHolder(v);
        holder.itemView.setOnClickListener(view -> {
            if (mListener != null) {
                int position1 = holder.getAdapterPosition();
                mListener.onTrackSelected(mTracks.get(position1));
            }
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(final TrackHolder holder, int position) {
        final MediaDescriptionCompat track = mTracks.get(position).getDescription();
        final Bundle extras = track.getExtras();

        holder.title.setText(track.getTitle());
        holder.duration.setText(track.getSubtitle());

        if (extras != null) {
            long duration = extras.getLong(MediaItems.EXTRA_DURATION);
            holder.duration.setText(formatDuration(duration));

            long trackNumber = extras.getLong(MediaItems.EXTRA_TRACK_NUMBER);
            holder.trackNo.setText(String.valueOf(trackNumber));
        }
    }

    private static String formatDuration(long duration) {
        return DateUtils.formatElapsedTime(duration / 1000);
    }

    @Override
    public long getItemId(int position) {
        if (hasStableIds()) {
            final String mediaId = mTracks.get(position).getMediaId();
            return Long.parseLong(MediaID.extractMusicID(mediaId));
        }
        return ListView.NO_ID;
    }

    void updateTracks(List<MediaItem> tracks) {
        MediaItemDiffCallback callback = new MediaItemDiffCallback(mTracks, tracks);
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(callback, false);
        mTracks.clear();
        mTracks.addAll(tracks);
        result.dispatchUpdatesTo(this);
    }

    public List<MediaItem> getItems() {
        return mTracks;
    }

    @Override
    public int getItemCount() {
        return mTracks.size();
    }

    public MediaItem get(int position) {
        return mTracks.get(position);
    }

    void setOnTrackSelectedListener(OnTrackSelectedListener listener) {
        mListener = listener;
    }

    interface OnTrackSelectedListener {
        void onTrackSelected(MediaItem track);
    }

    static class TrackHolder extends RecyclerView.ViewHolder {
        final TextView trackNo;
        final TextView title;
        final TextView duration;

        TrackHolder(View itemView) {
            super(itemView);
            trackNo = itemView.findViewById(R.id.trackNo);
            title = itemView.findViewById(R.id.title);
            duration = itemView.findViewById(R.id.duration);
        }
    }
}
