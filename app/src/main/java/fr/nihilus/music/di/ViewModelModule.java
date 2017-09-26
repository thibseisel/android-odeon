package fr.nihilus.music.di;

import android.arch.lifecycle.ViewModel;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;
import fr.nihilus.music.library.BrowserViewModel;

@Module
abstract class ViewModelModule {

    @Binds @IntoMap
    @ViewModelKey(BrowserViewModel.class)
    abstract ViewModel bindsBrowserViewModel(BrowserViewModel viewModel);
}
