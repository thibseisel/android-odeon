package fr.nihilus.music.glide.palette

import android.annotation.SuppressLint
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.util.Util

class PaletteBitmapResource(
        private val paletteBitmap: PaletteBitmap,
        private val bitmapPool: BitmapPool
) : Resource<PaletteBitmap> {

    override fun getResourceClass() = PaletteBitmap::class.java

    override fun get() = this.paletteBitmap

    @SuppressLint("NewApi")
    override fun getSize() = Util.getBitmapByteSize(paletteBitmap.bitmap)

    override fun recycle() = bitmapPool.put(paletteBitmap.bitmap)
}
