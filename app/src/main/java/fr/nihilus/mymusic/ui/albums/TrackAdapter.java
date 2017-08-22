package fr.nihilus.mymusic.ui.albums;

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

import java.util.List;

import fr.nihilus.mymusic.R;
import fr.nihilus.mymusic.media.MediaItems;
import fr.nihilus.mymusic.utils.MediaID;
import fr.nihilus.mymusic.utils.MediaItemDiffCallback;

class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackHolder> {

    private static final String TAG = "TrackAdapter";

    private List<MediaItem> mTracks;
    private OnTrackSelectedListener mListener;

    TrackAdapter(List<MediaItem> tracks) {
        mTracks = tracks;
    }

    @Override
    public TrackHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.album_track_item, parent, false);
        return new TrackHolder(v);
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

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    int position = holder.getAdapterPosition();
                    mListener.onTrackSelected(mTracks.get(position));
                }
            }
        });
    }

    private String formatDuration(long duration) {
        return DateUtils.formatElapsedTime(duration / 1000);
    }

    @Override
    public long getItemId(int position) {
        if (hasStableIds() && mTracks != null) {
            final String mediaId = mTracks.get(position).getMediaId();
            return Long.parseLong(MediaID.extractMusicID(mediaId));
        }
        return ListView.NO_ID;
    }

    void updateTracks(List<MediaItem> tracks) {
        MediaItemDiffCallback callback = new MediaItemDiffCallback(mTracks, tracks);
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(callback, false);
        mTracks = tracks;
        result.dispatchUpdatesTo(this);
    }

    @Override
    public int getItemCount() {
        return mTracks != null ? mTracks.size() : 0;
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
