package fr.nihilus.mymusic.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import fr.nihilus.mymusic.R;

public class CurrentlyPlayingDecoration extends RecyclerView.ItemDecoration {

    private final int mPaddingStart;
    private Drawable mIcon;
    private int mDecoratedPosition;

    public CurrentlyPlayingDecoration(@NonNull Context context) {
        mIcon = AppCompatResources.getDrawable(context, R.drawable.currently_playing_decoration);
        mDecoratedPosition = RecyclerView.NO_POSITION;
        mPaddingStart = context.getResources().getDimensionPixelSize(R.dimen.list_item_horizontal_padding);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (mDecoratedPosition != RecyclerView.NO_POSITION) {
            final View child = parent.getChildAt(mDecoratedPosition);
            if (child != null) {
                int left = mPaddingStart;
                int top = child.getTop() + (child.getHeight() - mIcon.getIntrinsicHeight()) / 2;
                int right = left + mIcon.getIntrinsicWidth();
                int bottom = top + mIcon.getIntrinsicHeight();

                mIcon.setBounds(left, top, right, bottom);
                mIcon.draw(c);
            }
        }
    }

    public void setDecoratedItemPosition(@IntRange(from = -1) int position) {
        mDecoratedPosition = position;
    }

    public void setIconColor(@ColorInt int color) {
        mIcon = DrawableCompat.wrap(mIcon);
        DrawableCompat.setTint(mIcon, color);
    }
}
