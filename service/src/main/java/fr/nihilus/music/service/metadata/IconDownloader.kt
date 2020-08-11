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

package fr.nihilus.music.service.metadata

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.target.Target
import fr.nihilus.music.service.ServiceScoped
import fr.nihilus.music.service.extensions.intoBitmap
import javax.inject.Inject

/**
 * Provide a facility for loading icons from local and remote locations.
 * Depending on the implementation, icons may be cached in memory or on disk
 * to speed up further loading.
 */
internal interface IconDownloader {

    /**
     * Load a [Bitmap] from the specified [iconUri], and scale it to the specified dimensions.
     * Depending on the location pointed by the uri, icons may be loaded from network or device's storage.
     * Any load that fails will result in a `null` bitmap.
     *
     * If at least one of the dimensions of the loaded bitmap is larger than the one specified,
     * it will be downscaled while maintaining its original aspect ratio,
     * so that its largest dimension will be equal to the one specified.
     * If both dimensions are smaller than the ones specified then no upscaling occurs.
     *
     * @param iconUri The uri pointing to the icon resource to load.
     * @param width The desired width for the loaded bitmap in pixels,
     * or [ORIGINAL_SIZE] to use the original width.
     * @param height The desired height for the loaded bitmap in pixels,
     * or [ORIGINAL_SIZE] to use the original height.
     */
    suspend fun loadBitmap(iconUri: Uri, width: Int, height: Int): Bitmap?

    companion object {

        /**
         * Special value for the `width` or `height` parameter of [loadBitmap]
         * indicating that that dimension of the loaded bitmap should not be scaled.
         */
        const val ORIGINAL_SIZE: Int = Target.SIZE_ORIGINAL
    }
}

/**
 * An icon downloader that uses [Glide] to load icons from the web or the device's storage.
 * Glide will take care caching icons that have been previously loaded.
 *
 * @param context The context used to initialize Glide.
 */
@ServiceScoped
internal class GlideDownloader @Inject constructor(context: Context) : IconDownloader {
    private val glide: RequestBuilder<Bitmap> = Glide.with(context)
        .asBitmap()
        .disallowHardwareConfig()
        .downsample(DownsampleStrategy.CENTER_INSIDE)
        .lock()

    override suspend fun loadBitmap(iconUri: Uri, width: Int, height: Int): Bitmap? {
        require(width >= 0 || width == IconDownloader.ORIGINAL_SIZE) {
            "Width must be > 0 or IconDownloader.ORIGINAL_SIZE, but was $width"
        }
        require(height >= 0 || height == IconDownloader.ORIGINAL_SIZE) {
            "Height must be > 0 or IconDownloader.ORIGINAL_SIZE, but was $height"
        }

        return glide.load(iconUri).intoBitmap(width, height)
    }
}