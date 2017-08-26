package fr.nihilus.mymusic.service;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fr.nihilus.mymusic.di.ServiceScoped;

@Module
public abstract class MusicServiceModule {

    @ServiceScoped
    @ContributesAndroidInjector
    abstract MusicService contributeMusicService();
}
