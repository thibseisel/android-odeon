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

import android.content.Context
import android.media.AudioManager
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import dagger.Module
import dagger.Provides
import dagger.Reusable
import fr.nihilus.music.media.playback.AudioOnlyRenderersFactory
import fr.nihilus.music.media.service.MusicService

@Module(includes = [ServiceBindingsModule::class])
internal object PlaybackModule {

    @JvmStatic
    @[Provides Reusable]
    fun provideExoPlayer(service: MusicService): SimpleExoPlayer = ExoPlayerFactory.newSimpleInstance(
        AudioOnlyRenderersFactory(service),
        DefaultTrackSelector()
    )

    @JvmStatic
    @Provides
    fun providesAudioManager(context: Context): AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
}