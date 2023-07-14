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

import android.app.Service
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.util.EventLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.scopes.ServiceScoped
import fr.nihilus.music.service.BuildConfig

@Module
@InstallIn(ServiceComponent::class)
@OptIn(UnstableApi::class)
internal object PlaybackModule {

    @Provides @ServiceScoped
    fun provideExoPlayer(context: Service): ExoPlayer {
        val musicAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(
            context,
            AudioOnlyRenderersFactory(context),
            DefaultMediaSourceFactory(context, AudioOnlyExtractorsFactory())
        )
            .setAudioAttributes(musicAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setUsePlatformDiagnostics(false)
            .build()

        if (BuildConfig.DEBUG) {
            // Print player logs on debug builds.
            player.addAnalyticsListener(EventLogger())
        }

        return player
    }
}
