package fr.nihilus.mymusic.palette;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;

/**
 * A transcoder that allows the Glide library to calculate the color palette associated
 * with the loaded bitmap on a background thread.
 * This implementation only extracts colors from the bottom of the bitmap.
 */
public class BottomPaletteTranscoder extends PaletteBitmapTranscoder {

    public BottomPaletteTranscoder(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    protected Palette onGeneratePalette(Bitmap bitmap) {
        return Palette.from(bitmap)
                .setRegion(0, 3 * bitmap.getHeight() / 4, bitmap.getWidth(), bitmap.getHeight())
                .maximumColorCount(16)
                .generate();
    }
}
