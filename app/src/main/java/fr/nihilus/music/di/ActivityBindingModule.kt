/*
 * Copyright 2017 Thibault Seisel
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
import fr.nihilus.music.HomeActivity
import fr.nihilus.music.settings.SettingsActivity
import fr.nihilus.music.settings.SettingsModule
import fr.nihilus.music.ui.albums.AlbumDetailActivity

/**
 * We want Dagger.Android to create a Subcomponent which has a parent Component
 * of whichever module ActivityBindingModule is on, in our case that will be AppComponent.
 * The beautiful part about this setup is that you never need to tell AppComponent
 * that it is going to have all these subcomponents nor do you need to tell these subcomponents
 * that AppComponent exists.
 *
 * We are also telling Dagger.Android that this generated SubComponent needs to include the
 * specified modules and be aware of a scope annotation @ActivityScoped
 *
 * When Dagger.Android annotation processor runs it will create 3 subcomponents for us.
 */
@Suppress("unused")
@Module
abstract class ActivityBindingModule {

    @ActivityScoped
    @ContributesAndroidInjector(modules = arrayOf(MusicLibraryModule::class, ViewModelModule::class))
    abstract fun contributeHomeActivity(): HomeActivity

    @ActivityScoped
    @ContributesAndroidInjector(modules = arrayOf(ViewModelModule::class))
    abstract fun contributeAlbumDetailActivity(): AlbumDetailActivity

    @ActivityScoped
    @ContributesAndroidInjector(modules = arrayOf(SettingsModule::class))
    abstract fun contributeSettingsActivity(): SettingsActivity
}
