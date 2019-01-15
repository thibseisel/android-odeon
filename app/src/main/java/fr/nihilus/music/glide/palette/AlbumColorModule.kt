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

import android.content.Context
import androidx.core.content.ContextCompat
import dagger.Module
import dagger.Provides
import fr.nihilus.music.library.albums.AlbumPalette

@Module
class AlbumColorModule {

    @Provides
    fun providesDefaultAlbumPalette(context: Context) =
        AlbumPalette(
            primary = ContextCompat.getColor(context, R.color.album_band_default),
            accent = ContextCompat.getColor(context, R.color.color_accent),
            titleText = ContextCompat.getColor(context, android.R.color.white),
            bodyText = ContextCompat.getColor(context, android.R.color.white),
            textOnAccent = ContextCompat.getColor(context, R.color.color_control_normal_compat)
        )
}
