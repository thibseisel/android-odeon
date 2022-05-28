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

package fr.nihilus.music.extensions

import android.content.Context
import androidx.core.content.res.getColorOrThrow
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.glide.palette.AlbumPalette
import fr.nihilus.music.core.ui.R as CoreUiR

fun Context.resolveDefaultAlbumPalette(): AlbumPalette {
    val attrs = obtainStyledAttributes(CoreUiR.styleable.DefaultAlbumPalette)
    return try {
        AlbumPalette(
            attrs.getColorOrThrow(CoreUiR.styleable.DefaultAlbumPalette_albumPalettePrimaryColor),
            attrs.getColorOrThrow(CoreUiR.styleable.DefaultAlbumPalette_albumPaletteAccentColor),
            attrs.getColorOrThrow(CoreUiR.styleable.DefaultAlbumPalette_albumPaletteTitleTextColor),
            attrs.getColorOrThrow(CoreUiR.styleable.DefaultAlbumPalette_albumPaletteBodyTextColor),
            attrs.getColorOrThrow(CoreUiR.styleable.DefaultAlbumPalette_albumPaletteTextOnAccentColor)
        )
    } finally {
        attrs.recycle()
    }
}
