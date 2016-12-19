package fr.nihilus.mymusic.palette;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;

public class PaletteBitmap {

    public final Palette palette;
    public final Bitmap bitmap;

    PaletteBitmap(@NonNull Palette palette, @NonNull Bitmap bitmap) {
        this.palette = palette;
        this.bitmap = bitmap;
    }
}
