package fr.nihilus.music.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import fr.nihilus.music.HomeActivity
import fr.nihilus.music.settings.SettingsActivity
import fr.nihilus.music.settings.SettingsModule
import fr.nihilus.music.ui.albums.AlbumDetailActivity

/**
 * We want Dagger.Android to create a Subcomponent which has a parent Component
 * of whichever module ActivityBindingModule is on, in our case that will be AppComponent.
 * The beautiful part about this setup is that you never need to tell AppComponent
 * that it is going to have all these subcomponents nor do you need to tell these subcomponents
 * that AppComponent exists.
 *
 * We are also telling Dagger.Android that this generated SubComponent needs to include the
 * specified modules and be aware of a scope annotation @ActivityScoped
 *
 * When Dagger.Android annotation processor runs it will create 3 subcomponents for us.
 */
@Module
abstract class ActivityBindingModule {

    @ActivityScoped
    @ContributesAndroidInjector(modules = arrayOf(MusicLibraryModule::class, ViewModelModule::class))
    abstract fun contributeHomeActivity(): HomeActivity

    @ActivityScoped
    @ContributesAndroidInjector(modules = arrayOf(ViewModelModule::class))
    abstract fun contributeAlbumDetailActivity(): AlbumDetailActivity

    @ActivityScoped
    @ContributesAndroidInjector(modules = arrayOf(SettingsModule::class))
    abstract fun contributeSettingsActivity(): SettingsActivity
}
