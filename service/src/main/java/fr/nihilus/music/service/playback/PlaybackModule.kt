/*
 * Copyright 2021 Thibault Seisel
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

package fr.nihilus.music.service.playback

import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.util.EventLogger
import dagger.Module
import dagger.Provides
import fr.nihilus.music.service.BuildConfig
import fr.nihilus.music.service.MusicService
import fr.nihilus.music.service.ServiceBindingsModule
import fr.nihilus.music.service.ServiceScoped

@Module(includes = [ServiceBindingsModule::class])
internal object PlaybackModule {

    @Provides @ServiceScoped
    fun provideExoPlayer(context: MusicService): ExoPlayer {
        val musicAttributes = AudioAttributes.Builder()
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = SimpleExoPlayer.Builder(
            context,
            AudioOnlyRenderersFactory(context),
            AudioOnlyExtractorsFactory()
        )
            .setAudioAttributes(musicAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        if (BuildConfig.DEBUG) {
            // Print player logs on debug builds.
            player.addAnalyticsListener(EventLogger(null))
            player.setThrowsWhenUsingWrongThread(true)
        }

        return player
    }
}