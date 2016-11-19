package fr.nihilus.mymusic.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import fr.nihilus.mymusic.R;
import fr.nihilus.mymusic.utils.MediaIDHelper;
import fr.nihilus.mymusic.widget.AlphabetIndexer;

class SongAdapter extends BaseAdapter /*implements SectionIndexer*/ {

    private static final String TAG = "SongAdapter";

    private final Context mContext;
    private final LayoutInflater mInflater;
    private final SectionIndexer mIndexer;
    private List<MediaBrowserCompat.MediaItem> mSongs;
    private final Drawable mDummyAlbumArt;

    SongAdapter(@NonNull Context ctx, @NonNull List<MediaBrowserCompat.MediaItem> songs) {
        mContext = ctx;
        mInflater = LayoutInflater.from(ctx);
        mSongs = songs;
        mIndexer = new AlphabetIndexer(mSongs);
        mDummyAlbumArt = ContextCompat.getDrawable(ctx, R.drawable.dummy_album_art);
    }

    @Override
    public int getCount() {
        return mSongs != null ? mSongs.size() : 0;
    }

    @Override
    public MediaBrowserCompat.MediaItem getItem(int pos) {
        return mSongs != null ? mSongs.get(pos) : null;
    }

    @Override
    public long getItemId(int pos) {
        if (hasStableIds() && mSongs != null) {
            String mediaId = mSongs.get(pos).getMediaId();
            return Long.parseLong(MediaIDHelper.extractMusicIDFromMediaID(mediaId));
        }
        return ListView.NO_ID;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.song_list_item, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        MediaDescriptionCompat song = mSongs.get(pos).getDescription();
        holder.title.setText(song.getTitle());
        holder.subtitle.setText(song.getSubtitle());

        Glide.with(mContext).load(song.getIconUri()).asBitmap()
                .error(mDummyAlbumArt)
                .into(holder.albumArt);

        return convertView;
    }

    /*@Override
    public Object[] getSections() {
        return mIndexer.getSections();
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        return mIndexer.getPositionForSection(sectionIndex);
    }

    @Override
    public int getSectionForPosition(int position) {
        return mIndexer.getSectionForPosition(position);
    }*/

    private static class ViewHolder {
        TextView title;
        TextView subtitle;
        ImageView albumArt;

        ViewHolder(View root) {
            title = (TextView) root.findViewById(R.id.title);
            subtitle = (TextView) root.findViewById(R.id.subtitle);
            albumArt = (ImageView) root.findViewById(R.id.albumArt);
        }
    }
}
