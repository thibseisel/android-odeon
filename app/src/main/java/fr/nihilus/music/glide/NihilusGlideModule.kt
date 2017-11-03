package fr.nihilus.music.glide

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import fr.nihilus.music.glide.palette.PaletteBitmap
import fr.nihilus.music.glide.palette.PaletteBitmapTranscoder

@GlideModule
class NihilusGlideModule : AppGlideModule() {
    override fun isManifestParsingEnabled() = false

    override fun registerComponents(context: Context?, glide: Glide?, registry: Registry?) {
        if (registry != null && glide != null) {
            // Calculate the color Palette associated with the loaded Bitmap
            registry.register(Bitmap::class.java, PaletteBitmap::class.java,
                    PaletteBitmapTranscoder(glide.bitmapPool))
        }
    }
}
