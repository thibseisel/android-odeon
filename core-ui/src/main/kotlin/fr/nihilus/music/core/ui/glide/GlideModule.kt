/*
 * Copyright 2020 Thibault Seisel
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

package fr.nihilus.music.core.ui.glide

import android.content.Context
import android.os.Build
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.resource.bitmap.BitmapEncoder
import com.bumptech.glide.load.resource.bitmap.ByteBufferBitmapDecoder
import com.bumptech.glide.load.resource.bitmap.Downsampler
import com.bumptech.glide.load.resource.bitmap.StreamBitmapDecoder
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import fr.nihilus.music.core.ui.glide.palette.AlbumArt
import fr.nihilus.music.core.ui.glide.palette.AlbumArtEncoder
import fr.nihilus.music.core.ui.glide.palette.BufferAlbumArtDecoder
import fr.nihilus.music.core.ui.glide.palette.StreamAlbumArtDecoder
import java.io.InputStream
import java.nio.ByteBuffer

@GlideModule
class GlideModule : AppGlideModule() {

    override fun isManifestParsingEnabled() = false

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        val bitmapEncoder = BitmapEncoder(glide.arrayPool)
        val downSampler = Downsampler(
            registry.imageHeaderParsers,
            context.resources.displayMetrics,
            glide.bitmapPool,
            glide.arrayPool
        )
        val bufferBitmapDecoder = ByteBufferBitmapDecoder(downSampler)
        val streamBitmapDecoder = StreamBitmapDecoder(downSampler, glide.arrayPool)

        // Decode AlbumArts from source or cache, generating an AlbumPalette only when required.
        registry.append(
            ByteBuffer::class.java,
            AlbumArt::class.java,
            BufferAlbumArtDecoder(bufferBitmapDecoder)
        )

        registry.append(
            InputStream::class.java,
            AlbumArt::class.java,
            StreamAlbumArtDecoder(streamBitmapDecoder)
        )

        // Store loaded AlbumArts to the disk cache.
        registry.append(
            AlbumArt::class.java,
            AlbumArtEncoder(bitmapEncoder, glide.bitmapPool, glide.arrayPool)
        )
    }

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // Use Hardware Bitmaps whenever possible.
        // Hardware Bitmaps were introduced in Android O
        // but don't work with Shared Element Transitions until O MR1.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            builder.setDefaultRequestOptions(
                RequestOptions.formatOf(DecodeFormat.PREFER_ARGB_8888)
            )
        }
    }
}