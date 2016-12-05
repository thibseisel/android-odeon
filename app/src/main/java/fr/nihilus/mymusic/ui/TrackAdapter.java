package fr.nihilus.mymusic.ui;

import android.content.Context;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import fr.nihilus.mymusic.R;
import fr.nihilus.mymusic.utils.MediaIDHelper;

class TrackAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final LayoutInflater mInflater;
    private List<MediaItem> mTracks;
    private OnTrackSelectedListener mListener;

    TrackAdapter(Context context, List<MediaItem> tracks) {
        mInflater = LayoutInflater.from(context);
        mTracks = tracks;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = mInflater.inflate(R.layout.track_list_item, parent, false);
        return new TrackHolder(v);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        final TrackHolder trackHolder = (TrackHolder) holder;
        final MediaDescriptionCompat track = mTracks.get(position).getDescription();
        trackHolder.title.setText(track.getTitle());
        trackHolder.info.setText(track.getDescription());
        // TODO Numéro de piste

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onTrackSelected(mTracks.get(holder.getAdapterPosition()));
                }
            }
        });
    }

    @Override
    public long getItemId(int position) {
        if (hasStableIds() && mTracks != null) {
            final String mediaId = mTracks.get(position).getMediaId();
            return Long.parseLong(MediaIDHelper.extractMusicIDFromMediaID(mediaId));
        }
        return ListView.NO_ID;
    }

    @Override
    public int getItemCount() {
        return mTracks != null ? mTracks.size() : 0;
    }

    void setOnTrackSelectedListener(OnTrackSelectedListener listener) {
        mListener = listener;
    }

    private static class TrackHolder extends RecyclerView.ViewHolder {
        final TextView trackNo;
        final TextView title;
        final TextView info;

        TrackHolder(View itemView) {
            super(itemView);
            trackNo = (TextView) itemView.findViewById(R.id.trackNo);
            title = (TextView) itemView.findViewById(R.id.title);
            info = (TextView) itemView.findViewById(R.id.info);
        }
    }

    interface OnTrackSelectedListener {
        void onTrackSelected(MediaItem track);
    }
}
