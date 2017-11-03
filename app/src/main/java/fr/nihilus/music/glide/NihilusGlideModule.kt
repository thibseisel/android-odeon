package fr.nihilus.music.glide

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule

@GlideModule
class NihilusGlideModule : AppGlideModule() {
    override fun isManifestParsingEnabled() = false

    override fun registerComponents(context: Context?, glide: Glide?, registry: Registry?) {
        if (registry != null) {

            if (glide != null) {
                // Calculate the color Palette associated with the loaded Bitmap
                registry.register(Bitmap::class.java, PaletteBitmap::class.java,
                        PaletteBitmapTranscoder(glide.bitmapPool))
            }

            if (context != null) {
                // Wrap the loaded Drawable in a StateListDrawable
                registry.register(Drawable::class.java, StateListDrawable::class.java,
                        StateListDrawableTranscoder(context))
            }
        }
    }
}
