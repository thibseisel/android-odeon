/*
 * Copyright 2017 Thibault Seisel
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

package fr.nihilus.music.service;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.util.ErrorMessageProvider;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;
import fr.nihilus.music.command.CommandModule;
import fr.nihilus.music.di.ServiceScoped;
import fr.nihilus.music.playback.AudioFocusAwarePlayer;
import fr.nihilus.music.playback.ErrorHandler;

@Module(includes = {CommandModule.class})
public abstract class MusicServiceModule {

    @ServiceScoped
    @ContributesAndroidInjector
    abstract MusicService contributeMusicService();

    @Provides
    static SimpleExoPlayer provideExoPlayer(@NonNull Context context) {
        return ExoPlayerFactory.newSimpleInstance(context, new DefaultTrackSelector());
    }

    @Binds
    abstract ExoPlayer bindsExoPlayer(AudioFocusAwarePlayer playerImpl);

    @Binds
    abstract ErrorMessageProvider<ExoPlaybackException> bindsErrorProvider(ErrorHandler handler);
}
