package fr.nihilus.mymusic.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fr.nihilus.mymusic.HomeActivity;
import fr.nihilus.mymusic.settings.SettingsActivity;
import fr.nihilus.mymusic.settings.SettingsModule;
import fr.nihilus.mymusic.ui.albums.AlbumDetailActivity;

/**
 * We want Dagger.Android to create a Subcomponent which has a parent Component
 * of whichever module ActivityBindingModule is on, in our case that will be AppComponent.
 * The beautiful part about this setup is that you never need to tell AppComponent
 * that it is going to have all these subcomponents nor do you need to tell these subcomponents
 * that AppComponent exists.
 * <p>We are also telling Dagger.Android that this generated SubComponent needs to include the
 * specified modules and be aware of a scope annotation @ActivityScoped
 * <p>When Dagger.Android annotation processor runs it will create 3 subcomponents for us.
 */
@Module
abstract class ActivityBindingModule {

    @ActivityScoped
    @ContributesAndroidInjector(modules = MusicLibraryModule.class)
    abstract HomeActivity contributeHomeActivity();

    @ActivityScoped
    @ContributesAndroidInjector
    abstract AlbumDetailActivity contributeAlbumDetailActivity();

    @ActivityScoped
    @ContributesAndroidInjector(modules = SettingsModule.class)
    abstract SettingsActivity contributeSettingsActivity();
}
