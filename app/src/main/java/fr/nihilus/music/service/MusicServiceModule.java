package fr.nihilus.music.service;

import dagger.Binds;
import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fr.nihilus.music.command.CommandModule;
import fr.nihilus.music.di.ServiceScoped;
import fr.nihilus.music.playback.ExoMusicPlayer;
import fr.nihilus.music.playback.MusicPlayer;

@Module(includes = {CommandModule.class})
public abstract class MusicServiceModule {

    @ServiceScoped
    @ContributesAndroidInjector
    abstract MusicService contributeMusicService();

    @Binds
    abstract MusicPlayer bindsExoPlayback(ExoMusicPlayer exoPlayback);
}
