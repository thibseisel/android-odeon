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
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import dagger.Module
import dagger.Provides
import fr.nihilus.music.media.playback.AudioOnlyRenderersFactory
import fr.nihilus.music.media.service.MusicService

@Module(includes = [ServiceBindingsModule::class])
internal object PlaybackModule {

    @JvmStatic
    @[Provides ServiceScoped]
    fun provideExoPlayer(context: MusicService): ExoPlayer = ExoPlayerFactory.newSimpleInstance(
        context,
        AudioOnlyRenderersFactory(context),
        DefaultTrackSelector()
    ).also {
        val musicAttributes = AudioAttributes.Builder()
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
        it.setAudioAttributes(musicAttributes, true)
    }

    @JvmStatic
    @Provides
    fun providesAudioManager(context: Context): AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
}