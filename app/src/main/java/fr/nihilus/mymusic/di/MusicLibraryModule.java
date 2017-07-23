package fr.nihilus.mymusic.di;

import android.content.Context;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;
import fr.nihilus.mymusic.HomeActivity;
import fr.nihilus.mymusic.ui.albums.AlbumDetailActivity;

@Module
public abstract class MusicLibraryModule {

    @ActivityScope
    @ContributesAndroidInjector(modules = {FragmentsBuilderModule.class})
    abstract HomeActivity contributeHomeActivity();

    @ActivityScope
    @ContributesAndroidInjector
    abstract AlbumDetailActivity contributeAlbumDetailActivity();

    @Provides
    @Named("HomeActivity")
    Context provideHomeActivityContext(HomeActivity activity) {
        return activity;
    }

    @Provides
    @Named("AlbumActivity")
    Context provideAlbumActivityContext(AlbumDetailActivity activity) {
        return activity;
    }
}
