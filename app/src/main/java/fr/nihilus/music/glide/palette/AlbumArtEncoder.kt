/*
 * Copyright 2018 Thibault Seisel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.nihilus.music.glide.palette

import android.graphics.Bitmap
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.ResourceEncoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import fr.nihilus.music.ui.albums.AlbumPalette
import timber.log.Timber
import java.io.File

class AlbumArtEncoder(
    private val encoder: ResourceEncoder<Bitmap>,
    private val bitmapPool: BitmapPool
) : ResourceEncoder<AlbumArt> {

    override fun getEncodeStrategy(options: Options) = encoder.getEncodeStrategy(options)

    override fun encode(data: Resource<AlbumArt>, file: File, options: Options): Boolean {
        Timber.d("Encoding AlbumArt to file ${file.name}")
        val (bitmap, _) = data.get()
        return encoder.encode(BitmapResource(bitmap, bitmapPool), file, options)
    }
}

class AlbumArtDecoder<DataType>(
    private val decoder: ResourceDecoder<DataType, Bitmap>,
    private val bitmapPool: BitmapPool
) : ResourceDecoder<DataType, AlbumArt> {

    override fun handles(source: DataType, options: Options) = decoder.handles(source, options)

    override fun decode(
        source: DataType,
        width: Int,
        height: Int,
        options: Options
    ): Resource<AlbumArt>? {
        Timber.d("Retrieving AlbumArt from $source")
        return decoder.decode(source, width, height, options)?.let {
            val albumArt = AlbumArt(it.get(), AlbumPalette(0, 0, 0, 0, 0))
            AlbumArtResource(albumArt, bitmapPool)
        }
    }

}