package fr.nihilus.music.glide

import android.graphics.Rect
import com.bumptech.glide.annotation.GlideExtension
import com.bumptech.glide.annotation.GlideOption
import com.bumptech.glide.request.RequestOptions

@GlideExtension
object PaletteExtensions {

    @GlideOption
    @JvmStatic fun maxColorCount(options: RequestOptions, maxColorCount: Int) {
        if (maxColorCount < 1) {
            throw IllegalArgumentException("Invalid maxColorCount: $maxColorCount")
        }

        options.set(PaletteBitmapTranscoder.MAX_COLOR_COUNT, maxColorCount)
    }

    @GlideOption
    @JvmStatic fun region(options: RequestOptions, left: Int, top: Int, right: Int, bottom: Int) {
        val rect = Rect(left, top, right, bottom)
        options.set(PaletteBitmapTranscoder.PALETTE_REGION, rect)
    }
}