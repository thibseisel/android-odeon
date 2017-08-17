package fr.nihilus.mymusic.ui.songs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.bumptech.glide.BitmapRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

import fr.nihilus.mymusic.R;
import fr.nihilus.mymusic.utils.MediaID;
import fr.nihilus.mymusic.utils.MediaItemIndexer;

public class SongAdapter extends BaseAdapter implements SectionIndexer {

    private static final String TAG = "SongAdapter";

    private final BitmapRequestBuilder<Uri, Bitmap> mGlideRequest;
    private final MediaItemIndexer mIndexer;
    private List<MediaBrowserCompat.MediaItem> mSongs;

    public SongAdapter(@NonNull Context context, List<MediaBrowserCompat.MediaItem> songs) {
        mSongs = songs;
        mIndexer = new MediaItemIndexer(songs);
        registerDataSetObserver(mIndexer);
        Drawable dummyAlbumArt = ContextCompat.getDrawable(context, R.drawable.ic_audiotrack_24dp);
        mGlideRequest = Glide.with(context)
                .fromUri()
                .asBitmap()
                .error(dummyAlbumArt)
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.NONE);
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
            return Long.parseLong(MediaID.extractMusicID(mediaId));
        }
        return ListView.NO_ID;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.song_list_item, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final Context context = holder.title.getContext();
        final MediaDescriptionCompat song = mSongs.get(pos).getDescription();

        // TODO Stocker la durée formattée dans les extras et la récupérer ici au lieu de la calculer
        //noinspection ConstantConditions
        final long millis = song.getExtras().getLong(MediaStore.Audio.AudioColumns.DURATION);
        final CharSequence duration = DateUtils.formatElapsedTime(millis / 1000);
        String subtitle = context.getString(R.string.song_item_subtitle, song.getSubtitle(), duration);

        holder.title.setText(song.getTitle());
        holder.subtitle.setText(subtitle);
        mGlideRequest.load(song.getIconUri()).into(holder.albumArt);

        return convertView;
    }

    @Override
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
    }

    private static class ViewHolder {
        final TextView title;
        final TextView subtitle;
        final ImageView albumArt;

        ViewHolder(View root) {
            title = root.findViewById(R.id.title);
            subtitle = root.findViewById(R.id.subtitle);
            albumArt = root.findViewById(R.id.cover);
        }
    }
}
