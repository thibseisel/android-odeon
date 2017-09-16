package fr.nihilus.music.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

public final class ViewUtils {

    public static void setLightStatusBar(@NonNull View view, boolean darkIcons) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view.setSystemUiVisibility(darkIcons ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : 0);
        }
    }

    /**
     * Indique si une couleur est claire.
     * Cette définition s'appuie sur le niveau de saturation et la luminosité.
     *
     * @param color couleur à tester
     * @return true si la couleur est claire, false si elle est foncée
     */
    public static boolean isColorBright(@ColorInt int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        return (hsv[1] < 0.5) && (hsv[2] > 0.5);
    }

    @ColorInt
    public static int darker(@ColorInt int color, @FloatRange(from = 0.0, to = 1.0) float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= factor;
        return Color.HSVToColor(hsv);
    }

    /**
     * Donne le nombre de pixels équivalent à un nombre de DIP (Density-Independant Pixel).
     * Cette unité permet d'obtenir des dimensions similaires sur des écrans de taille
     * et de densité différentes.
     *
     * @param dp nombre de dp à convertir en pixels
     * @return nombre de pixels
     */
    public static int dipToPixels(Context context, float dp) {
        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics));
    }

    /**
     * Récupère une couleur appartenant au thème de l'application, telle que "colorPrimary"
     * ou "colorAccent".
     *
     * @param themeAttr attribut du thème pointant sur la couleur
     * @return nombre entier représentant cette couleur
     */
    @ColorInt
    public static int resolveThemeColor(Context context, @AttrRes int themeAttr) {
        TypedValue outValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(themeAttr, outValue, true);
        return outValue.data;
    }
}
