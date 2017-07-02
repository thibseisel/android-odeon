package fr.nihilus.mymusic.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fr.nihilus.mymusic.service.MusicService;

@Module
public abstract class MusicServiceModule {

    @MusicServiceScope
    @ContributesAndroidInjector
    abstract MusicService contributeMusicService();
}
