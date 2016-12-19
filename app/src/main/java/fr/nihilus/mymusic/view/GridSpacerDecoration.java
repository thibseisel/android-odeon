package fr.nihilus.mymusic.view;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.Dimension;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

/**
 * An {@link RecyclerView.ItemDecoration} that adds spacing between items in a {@link RecyclerView}
 * whose LayoutManager is a {@link android.support.v7.widget.GridLayoutManager} or a
 * {@link android.support.v7.widget.StaggeredGridLayoutManager}.
 */
public class GridSpacerDecoration extends RecyclerView.ItemDecoration {

    private static final int DEFAULT_SPACING_DP = 1;
    private final int mSpace;

    public GridSpacerDecoration(@NonNull Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        mSpace = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                DEFAULT_SPACING_DP, dm));
    }

    public GridSpacerDecoration(@NonNull DisplayMetrics metrics, @Dimension int spaceDp) {
        mSpace = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                spaceDp, metrics));
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.left = mSpace;
        outRect.right = mSpace;
        outRect.bottom = mSpace;
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top = mSpace;
        }
    }
}
