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

package fr.nihilus.music.glide

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.resource.bitmap.BitmapEncoder
import com.bumptech.glide.load.resource.bitmap.ByteBufferBitmapDecoder
import com.bumptech.glide.load.resource.bitmap.Downsampler
import com.bumptech.glide.load.resource.bitmap.StreamBitmapDecoder
import com.bumptech.glide.module.AppGlideModule
import fr.nihilus.music.glide.palette.AlbumArt
import fr.nihilus.music.glide.palette.AlbumArtEncoder
import fr.nihilus.music.glide.palette.BufferAlbumArtDecoder
import fr.nihilus.music.glide.palette.StreamAlbumArtDecoder
import java.io.InputStream
import java.nio.ByteBuffer

@GlideModule
class GlideModule : AppGlideModule() {

    override fun isManifestParsingEnabled() = false

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        val bitmapEncoder = BitmapEncoder(glide.arrayPool)
        val downsampler = Downsampler(
            registry.imageHeaderParsers,
            context.resources.displayMetrics,
            glide.bitmapPool,
            glide.arrayPool
        )
        val bufferBitmapDecoder = ByteBufferBitmapDecoder(downsampler)
        val streamBitmapDecoder = StreamBitmapDecoder(downsampler, glide.arrayPool)

        // Decode AlbumArts from source or cache, generating an AlbumPalette only when required.
        registry.append(
            ByteBuffer::class.java,
            AlbumArt::class.java,
            BufferAlbumArtDecoder(context, bufferBitmapDecoder)
        )

        registry.append(
            InputStream::class.java,
            AlbumArt::class.java,
            StreamAlbumArtDecoder(context, streamBitmapDecoder)
        )

        // Store loaded AlbumArts to the disk cache.
        registry.append(
            AlbumArt::class.java,
            AlbumArtEncoder(context, bitmapEncoder, glide.bitmapPool, glide.arrayPool)
        )
    }
}