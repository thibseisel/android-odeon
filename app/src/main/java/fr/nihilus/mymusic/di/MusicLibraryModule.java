package fr.nihilus.mymusic.di;

import dagger.Module;
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
}
