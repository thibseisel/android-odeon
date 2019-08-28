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

package fr.nihilus.music.media.di

import android.content.ContentResolver
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import fr.nihilus.music.media.AppDispatchers
import fr.nihilus.music.media.RxSchedulers
import fr.nihilus.music.media.actions.CustomActionModule
import fr.nihilus.music.media.database.DatabaseModule
import fr.nihilus.music.media.service.MediaSessionModule
import fr.nihilus.music.media.service.MusicService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import java.io.File
import javax.inject.Named
import javax.inject.Scope

/**
 * Denote that the annotated class or component is alive as long as the enclosing service
 * instance is alive.
 */
@Scope
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
internal annotation class ServiceScoped

/**
 * Configures [MusicService]-related bindings, providing dependency injection for the whole `media` library module.
 * This Dagger module should be installed in the root component.
 */
@Module
@Suppress("unused")
abstract class MediaServiceModule {

    @ServiceScoped
    @ContributesAndroidInjector(modules = [
        MusicServiceProvisions::class,
        ServiceBindingsModule::class,
        DatabaseModule::class,
        MediaSessionModule::class,
        MediaSourceModule::class,
        PlaybackModule::class,
        CustomActionModule::class
    ])
    abstract fun contributesMusicService(): MusicService
}

@Module
internal class MusicServiceProvisions {

    @Provides @ServiceScoped
    @Named("internal")
    fun provideInternalStorageRoot(service: MusicService): File = service.filesDir

    @Provides @ServiceScoped
    fun provideContentResolver(service: MusicService): ContentResolver = service.contentResolver

    @Provides @ServiceScoped
    fun provideRxSchedulers() = RxSchedulers(
        AndroidSchedulers.mainThread(),
        Schedulers.computation(),
        Schedulers.io(),
        Schedulers.single()
    )

    @Provides @ServiceScoped
    fun provideAppDispatchers() = AppDispatchers(
        Dispatchers.Main,
        Dispatchers.Default,
        Dispatchers.IO
    )
}