package fr.nihilus.mymusic.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fr.nihilus.mymusic.HomeActivity;

@Module
public abstract class MusicLibraryModule {

    @ActivityScope
    @ContributesAndroidInjector
    abstract HomeActivity contributeHomeActivity();
}
