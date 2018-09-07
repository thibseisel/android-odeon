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

package fr.nihilus.music.media.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector.*
import com.google.android.exoplayer2.ext.mediasession.RepeatModeActionProvider
import com.google.android.exoplayer2.util.ErrorMessageProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import fr.nihilus.music.media.R
import fr.nihilus.music.media.di.ServiceScoped
import fr.nihilus.music.media.playback.CustomPlaybackController
import fr.nihilus.music.media.playback.ErrorHandler
import fr.nihilus.music.media.playback.MediaQueueManager

/**
 * Configures and provides MediaSession-related dependencies.
 */
@Module(includes = [SessionConnectorModule::class])
class MediaSessionModule {

    /**
     * Creates a media session associated with the given [service].
     */
    @Provides @ServiceScoped
    fun providesMediaSession(service: MusicService): MediaSessionCompat {
        val session = MediaSessionCompat(service, "MediaSession")
        val showUiIntent = PendingIntent.getActivity(
            service.applicationContext,
            R.id.abc_request_start_media_activity,
            Intent(service.applicationContext, /* TODO packageManager.getLaunchIntentForPackage(...) */ null),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        session.setSessionActivity(showUiIntent)
        session.setRatingType(RatingCompat.RATING_NONE)
        return session
    }

    @Provides @ServiceScoped
    fun providesSessionConnector(
        player: ExoPlayer,
        mediaSession: MediaSessionCompat,
        controller: PlaybackController,
        preparer: PlaybackPreparer,
        navigator: QueueNavigator,
        errorHandler: ErrorMessageProvider<ExoPlaybackException>,
        customActions: Set<@JvmSuppressWildcards CustomActionProvider>

    ) = MediaSessionConnector(mediaSession, controller, false, null).apply {
        setPlayer(player, preparer, *customActions.toTypedArray())
        setQueueNavigator(navigator)
        setErrorMessageProvider(errorHandler)
    }

    @Provides @ServiceScoped
    fun providesRepeatModeAction(context: Context, player: ExoPlayer) =
        RepeatModeActionProvider(context, player)
}

@Module
@Suppress("unused")
abstract class SessionConnectorModule {

    @Binds
    abstract fun bindsPlaybackController(controller: CustomPlaybackController): PlaybackController

    @Binds
    abstract fun bindsPlaybackPreparer(preparer: MediaQueueManager): PlaybackPreparer

    @Binds
    abstract fun bindsQueueNavigator(navigator: MediaQueueManager): QueueNavigator

    @Binds
    abstract fun bindsErrorMessageProvider(handler: ErrorHandler): ErrorMessageProvider<ExoPlaybackException>

    @Binds @IntoSet
    abstract fun bindsRepeatModeAction(action: RepeatModeActionProvider): CustomActionProvider

    @Binds @IntoSet
    abstract fun bindsTrimSilenceAction(action: TrimSilenceActionProvider): CustomActionProvider
}
