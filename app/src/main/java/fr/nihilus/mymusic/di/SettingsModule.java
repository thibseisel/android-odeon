package fr.nihilus.mymusic.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fr.nihilus.mymusic.settings.MainPreferenceFragment;
import fr.nihilus.mymusic.settings.SettingsActivity;

@Module
public abstract class SettingsModule {

    @ContributesAndroidInjector
    abstract SettingsActivity contributeSettingsActivity();

    @ContributesAndroidInjector
    abstract MainPreferenceFragment contributeMainPreferenceFragment();
}
