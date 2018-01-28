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

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        // Calculate the color Palette associated with the loaded Bitmap
        registry.register(
            Bitmap::class.java, PaletteBitmap::class.java,
            PaletteBitmapTranscoder(glide.bitmapPool)
        )
    }
}
