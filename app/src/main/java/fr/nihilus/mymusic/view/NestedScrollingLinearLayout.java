package fr.nihilus.mymusic.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import fr.nihilus.mymusic.R;

/**
 * A {@link LinearLayout} that delegates scrolling to a nested scrolling parent,
 * such as NestedScrollView.
 */
public class NestedScrollingLinearLayout extends LinearLayout implements NestedScrollingChild {

    private final NestedScrollingChildHelper mChildHelper;

    public NestedScrollingLinearLayout(Context context) {
        this(context, null, 0);
    }

    public NestedScrollingLinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NestedScrollingLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mChildHelper = new NestedScrollingChildHelper(this);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.NestedScrollingLinearLayout, defStyleAttr, 0);
        try {
            ViewCompat.setNestedScrollingEnabled(this, a.getBoolean(
                    R.styleable.NestedScrollingLinearLayout_nestedScrollingEnabled, true));
        } finally {
            a.recycle();
        }
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed,
                dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    protected void onDetachedFromWindow() {
        mChildHelper.onDetachedFromWindow();
        super.onDetachedFromWindow();
    }

    @Override
    public void onStopNestedScroll(View child) {
        mChildHelper.onStopNestedScroll(child);
    }
}
