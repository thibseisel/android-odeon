package fr.nihilus.mymusic.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.TypedValue;

/**
 * Created by Thib on 12/11/2016.
 */

public class DrawableUtil {

    public static Drawable tintedDrawable(Drawable drawable, int color) {
        Drawable wrapDrawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(wrapDrawable, color);
        return wrapDrawable;
    }

    public static int getThemeColor(Context ctx, @AttrRes int resId) {
        TypedValue outValue = new TypedValue();
        ctx.getTheme().resolveAttribute(resId, outValue, true);
        return outValue.data;
    }
}
