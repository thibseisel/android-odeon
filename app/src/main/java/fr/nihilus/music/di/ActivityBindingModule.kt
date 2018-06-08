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

import dagger.Module
import dagger.android.ContributesAndroidInjector
import fr.nihilus.music.FileViewerActivity
import fr.nihilus.music.HomeActivity
import fr.nihilus.music.glide.palette.AlbumColorModule
import fr.nihilus.music.settings.SettingsActivity
import fr.nihilus.music.settings.SettingsModule
import fr.nihilus.music.ui.albums.AlbumDetailActivity

/**
 * Enable dependency injection for activities in the application.
 * Each activity defines its own scope by creating a subcomponent of AppComponent.
 */
@Suppress("unused")
@Module
abstract class ActivityBindingModule {

    @ActivityScoped
    @ContributesAndroidInjector(modules = [
        MusicLibraryModule::class,
        ViewModelModule::class,
        AlbumColorModule::class
    ])
    abstract fun contributeHomeActivity(): HomeActivity

    @ActivityScoped
    @ContributesAndroidInjector(modules = [
        ViewModelModule::class,
        AlbumColorModule::class
    ])
    abstract fun contributeAlbumDetailActivity(): AlbumDetailActivity

    @ActivityScoped
    @ContributesAndroidInjector(modules = [SettingsModule::class, ViewModelModule::class])
    abstract fun contributeSettingsActivity(): SettingsActivity

    @ActivityScoped
    @ContributesAndroidInjector(modules = [ViewModelModule::class])
    abstract fun contributeFileViewerActivity(): FileViewerActivity
}
