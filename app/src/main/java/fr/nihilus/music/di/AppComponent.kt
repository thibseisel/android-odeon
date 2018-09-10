/*
 * Copyright 2018 Thibault Seisel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.nihilus.music.di

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import dagger.android.support.AndroidSupportInjectionModule
import fr.nihilus.music.NihilusMusicApplication
import fr.nihilus.music.media.di.MediaServiceModule
import javax.inject.Singleton

/**
 * The top-level component for this application.
 * Every injectable object annotated with `Singleton` is bound to it.
 */
@Singleton
@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        AppModule::class,
        ActivityBindingModule::class,
        MediaServiceModule::class
    ]
)
interface AppComponent {

    /**
     * Generate a builder for the application component.
     * This builder binds instance of the Android Application to the newly created component in
     * order to provide it as a dependency to any other object.
     */
    @Component.Builder
    interface Builder {

        /**
         * Attach the instance of the application to the component to be created,
         * allowing it to be injected into other objects.
         */
        @BindsInstance
        fun application(app: Application): Builder

        fun build(): AppComponent
    }

    /**
     * Inject dependencies into the Android application.
     * After this call, fields annotated with `Inject` will be initialized.
     */
    fun inject(app: NihilusMusicApplication)
}
