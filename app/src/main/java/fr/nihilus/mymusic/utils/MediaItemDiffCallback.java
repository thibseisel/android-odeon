package fr.nihilus.mymusic.utils;

import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v7.util.DiffUtil;

import java.util.List;

public class MediaItemDiffCallback extends DiffUtil.Callback {

    private List<MediaItem> mOld;
    private List<MediaItem> mNew;

    public MediaItemDiffCallback(List<MediaItem> oldList, List<MediaItem> newList) {
        mOld = oldList;
        mNew = newList;
    }

    @Override
    public int getOldListSize() {
        return mOld.size();
    }

    @Override
    public int getNewListSize() {
        return mNew.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        String oldId = mOld.get(oldItemPosition).getMediaId();
        String newId = mNew.get(oldItemPosition).getMediaId();
        return oldId.equals(newId);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        MediaDescriptionCompat oldDesc = mOld.get(oldItemPosition).getDescription();
        MediaDescriptionCompat newDesc = mNew.get(newItemPosition).getDescription();
        boolean sameUri = oldDesc.getIconUri() == newDesc.getIconUri();
        boolean sameTitle = oldDesc.getTitle() == newDesc.getTitle();
        boolean sameSubtitle = oldDesc.getSubtitle() == newDesc.getSubtitle();
        return sameUri && sameTitle && sameSubtitle;
    }
}
