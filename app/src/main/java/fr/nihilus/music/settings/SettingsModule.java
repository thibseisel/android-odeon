package fr.nihilus.music.settings;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fr.nihilus.music.di.FragmentScoped;

@Module
public abstract class SettingsModule {

    @FragmentScoped
    @ContributesAndroidInjector
    abstract MainPreferenceFragment contributeMainPreferenceFragment();
}
