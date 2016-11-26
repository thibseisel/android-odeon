package fr.nihilus.mymusic.widget;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

public class GridSpacerDecoration extends RecyclerView.ItemDecoration {

    private static final int DEFAULT_SPACING_DP = 1;
    private final int mSpace;

    public GridSpacerDecoration(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        mSpace = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                DEFAULT_SPACING_DP, dm));
    }

    public GridSpacerDecoration(int spacePixel) {
        mSpace = spacePixel;
    }

    public GridSpacerDecoration(Context context, int spaceDp) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        mSpace = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, spaceDp, dm));
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.left = mSpace;
        outRect.top = mSpace;
        outRect.right = mSpace;
    }
}
