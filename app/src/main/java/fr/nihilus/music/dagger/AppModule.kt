/*
 * Copyright 2020 Thibault Seisel
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

package fr.nihilus.music.dagger

import android.content.Context
import dagger.Module
import dagger.Provides
import fr.nihilus.music.BuildConfig
import fr.nihilus.music.OdeonApplication
import javax.inject.Named

/**
 * The main module for this application.
 * It defines dependencies that cannot be instantiated with a constructor,
 * such as implementations for abstract types or calls to factory methods.
 *
 * All dependencies defined here can be used in both app modules: client and service.
 */
@Module
internal object AppModule {

    @Provides
    fun provideContext(application: OdeonApplication): Context = application.applicationContext

    @Provides @Named("APP_VERSION_NAME")
    fun providesVersionName() = BuildConfig.VERSION_NAME
}