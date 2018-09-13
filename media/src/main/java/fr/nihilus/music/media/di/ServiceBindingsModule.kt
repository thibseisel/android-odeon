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

package fr.nihilus.music.media.di

import com.google.android.exoplayer2.ExoPlayer
import dagger.Binds
import dagger.Module
import fr.nihilus.music.media.MediaSettings
import fr.nihilus.music.media.SharedPreferencesMediaSettings
import fr.nihilus.music.media.playback.AudioFocusAwarePlayer

@Module
@Suppress("unused")
internal abstract class ServiceBindingsModule {

    @Binds
    abstract fun bindsSettings(settings: SharedPreferencesMediaSettings): MediaSettings

    @Binds
    abstract fun bindsExoPlayer(player: AudioFocusAwarePlayer): ExoPlayer
}