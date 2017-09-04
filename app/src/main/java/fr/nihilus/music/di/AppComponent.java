package fr.nihilus.music.di;

import android.app.Application;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjectionModule;
import fr.nihilus.music.MyApplication;
import fr.nihilus.music.service.MusicServiceModule;

@Singleton
@Component(modules = {
        AndroidInjectionModule.class,
        AppModule.class,
        ActivityBindingModule.class,
        MusicServiceModule.class
})
public interface AppComponent {

    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder application(Application app);

        AppComponent build();
    }

    void inject(MyApplication myapp);
}
