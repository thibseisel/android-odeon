package fr.nihilus.mymusic.service;

import dagger.Binds;
import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fr.nihilus.mymusic.di.ServiceScoped;
import fr.nihilus.mymusic.playback.ExoPlayback;
import fr.nihilus.mymusic.playback.Playback;

@Module
public abstract class MusicServiceModule {

    @ServiceScoped
    @ContributesAndroidInjector
    abstract MusicService contributeMusicService();

    @Binds
    abstract Playback bindsExoPlayback(ExoPlayback exoPlayback);
}
