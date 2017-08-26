package fr.nihilus.mymusic.settings;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fr.nihilus.mymusic.di.FragmentScoped;

@Module
public abstract class SettingsModule {

    @FragmentScoped
    @ContributesAndroidInjector
    abstract MainPreferenceFragment contributeMainPreferenceFragment();
}
