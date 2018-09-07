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

package fr.nihilus.music.glide.palette;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import dagger.Module;
import dagger.Provides;
import fr.nihilus.music.R;
import fr.nihilus.music.media.di.ActivityScoped;
import fr.nihilus.music.ui.albums.AlbumPalette;

@Module
public class AlbumColorModule {

    @Provides @ActivityScoped
    public static AlbumPalette providesDefaultAlbumPalette(@NonNull Context context) {
        return new AlbumPalette(
                ContextCompat.getColor(context, R.color.album_band_default),
                ContextCompat.getColor(context, R.color.color_accent),
                ContextCompat.getColor(context, android.R.color.white),
                ContextCompat.getColor(context, android.R.color.white),
                ContextCompat.getColor(context, R.color.color_control_normal_compat)
        );
    }
}
