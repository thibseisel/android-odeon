package fr.nihilus.mymusic.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fr.nihilus.mymusic.provider.SetupService;

@Module
public abstract class SetupModule {

    @ContributesAndroidInjector
    abstract SetupService contributeSetupService();
}
