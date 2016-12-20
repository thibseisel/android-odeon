package fr.nihilus.mymusic.palette;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;

public class PaletteBitmapTranscoder implements ResourceTranscoder<Bitmap, PaletteBitmap> {
    private final BitmapPool bitmapPool;

    public PaletteBitmapTranscoder(@NonNull Context context) {
        this.bitmapPool = Glide.get(context).getBitmapPool();
    }

    @Override
    public Resource<PaletteBitmap> transcode(Resource<Bitmap> toTranscode) {
        Bitmap bitmap = toTranscode.get();
        Palette palette = onGeneratePalette(bitmap);
        PaletteBitmap result = new PaletteBitmap(palette, bitmap);
        return new PaletteBitmapResource(result, bitmapPool);
    }

    @NonNull
    protected Palette onGeneratePalette(Bitmap bitmap) {
        return new Palette.Builder(bitmap).generate();
    }

    @Override
    public String getId() {
        return PaletteBitmapTranscoder.class.getName();
    }
}
