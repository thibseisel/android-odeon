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

package fr.nihilus.music.service

import android.app.PendingIntent
import android.app.Service
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.util.ErrorMessageProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.scopes.ServiceScoped
import fr.nihilus.music.service.playback.ErrorHandler
import fr.nihilus.music.service.playback.MediaQueueManager
import fr.nihilus.music.service.playback.OdeonPlaybackPreparer

/**
 * Configures and provides MediaSession-related dependencies.
 */
@Module
@InstallIn(ServiceComponent::class)
@Suppress("unused")
internal abstract class MediaSessionModule {

    @Binds
    abstract fun bindsPlaybackPreparer(preparer: OdeonPlaybackPreparer): MediaSessionConnector.PlaybackPreparer

    @Binds
    abstract fun bindsQueueNavigator(navigator: MediaQueueManager): MediaSessionConnector.QueueNavigator

    @Binds
    abstract fun bindsErrorMessageProvider(handler: ErrorHandler): ErrorMessageProvider<ExoPlaybackException>

    companion object {

        /**
         * Creates a media session associated with the given [service].
         */
        @Provides @ServiceScoped
        fun providesMediaSession(service: Service): MediaSessionCompat {
            val launcherActivityIntent =
                service.packageManager.getLaunchIntentForPackage(service.packageName)
                    ?.apply { action = MusicService.ACTION_PLAYER_UI }
                    ?: error("Unable to resolve app's launcher activity")

            val sessionActivity = PendingIntent.getActivity(
                service.applicationContext,
                0,
                launcherActivityIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            return MediaSessionCompat(service, "MusicService").also {
                it.setRatingType(RatingCompat.RATING_NONE)
                it.setSessionActivity(sessionActivity)
            }
        }
    }
}
