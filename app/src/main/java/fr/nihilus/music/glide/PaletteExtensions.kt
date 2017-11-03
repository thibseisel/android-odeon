package fr.nihilus.music.glide

import android.annotation.SuppressLint
import android.graphics.RectF
import com.bumptech.glide.annotation.GlideExtension
import com.bumptech.glide.annotation.GlideOption
import com.bumptech.glide.request.RequestOptions
import fr.nihilus.music.glide.palette.PaletteBitmapTranscoder

@SuppressLint("CheckResult")
@GlideExtension
object PaletteExtensions {

    @GlideOption
    @JvmStatic fun maxColorCount(options: RequestOptions, maxColorCount: Int) {
        require(maxColorCount > 0) { "Invalid maxColorCount: $maxColorCount" }
        options.set(PaletteBitmapTranscoder.MAX_COLOR_COUNT, maxColorCount)
    }

    @GlideOption
    @JvmStatic fun region(options: RequestOptions, left: Float, top: Float, right: Float, bottom: Float) {
        require(left <= right) { "Invalid rectangle: X-coordinate of left > right" }
        require(top <= bottom) { "Invalid rectangle: Y-coordinate of top > bottom" }

        val rect = RectF(left, top, right, bottom)
        options.set(PaletteBitmapTranscoder.PALETTE_RELATIVE_REGION, rect)
    }
}