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

import android.graphics.Color
import androidx.annotation.Px
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.annotation.GlideExtension
import com.bumptech.glide.annotation.GlideOption
import com.bumptech.glide.annotation.GlideType
import com.bumptech.glide.load.Option
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.BaseRequestOptions
import fr.nihilus.music.glide.palette.AlbumArt
import fr.nihilus.music.library.albums.AlbumPalette

@GlideExtension
object GlideExtensions {

    /**
     * Generate an [AlbumPalette] from the loaded bitmap.
     */
    @[JvmStatic GlideType(AlbumArt::class)]
    fun asAlbumArt(builder: RequestBuilder<AlbumArt>) = builder

    /**
     * Applies [RoundedCorners] to all default types and throws an exception
     * if asked to transform an unknown type.
     * This will override previous calls to [BaseRequestOptions.dontTransform].
     *
     * @param radius The desired corner rounding, in pixels.
     *
     * @see RoundedCorners
     */
    @[JvmStatic GlideOption]
    fun roundedCorners(options: BaseRequestOptions<*>, @Px radius: Int): BaseRequestOptions<*> =
        options
            .downsample(DownsampleStrategy.FIT_CENTER)
            .transform(RoundedCorners(radius))

    /**
     * Specify the default colors to use when one or more colors
     * cannot be extracted from the loaded Bitmap.
     *
     * @param palette A set of colors that are used by default
     * when one or more color is not available.
     */
    @[JvmStatic GlideOption]
    fun fallbackColors(options: BaseRequestOptions<*>, palette: AlbumPalette): BaseRequestOptions<*> =
        options.set(OPTION_DEFAULT_PALETTE, palette)

    val OPTION_DEFAULT_PALETTE: Option<AlbumPalette> =
        Option.memory("fr.nihilus.music.BufferAlbumArtDecoder.DEFAULT_PALETTE",
            AlbumPalette(
                primary = 0xFFC4C4C,
                accent = Color.WHITE,
                titleText = Color.WHITE,
                bodyText = Color.WHITE,
                textOnAccent = Color.WHITE
            )
        )

}