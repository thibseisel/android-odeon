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

package fr.nihilus.music.dagger

import android.arch.lifecycle.ViewModel
import android.support.v4.media.MediaBrowserCompat.MediaItem
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import dagger.android.AndroidInjector
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import fr.nihilus.music.client.ViewModelKey
import fr.nihilus.music.glide.palette.AlbumColorModule
import fr.nihilus.music.library.albums.AlbumDetailActivity
import fr.nihilus.music.library.albums.AlbumDetailViewModel
import fr.nihilus.music.library.albums.AlbumPalette

/**
 * Define an Android Injector that injects dependencies into [AlbumDetailActivity].
 */
@ActivityScoped
@Subcomponent(modules = [
    ViewModelModule::class
])
interface AlbumDetailSubcomponent : AndroidInjector<AlbumDetailActivity> {

    /**
     * Defines how to create instances of [AlbumDetailSubcomponent].
     * This is used to provide any other dependencies that depends on [AlbumDetailActivity].
     */
    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<AlbumDetailActivity>() {

        /**
         * Provides the [MediaItem] of the album whose detail is to be displayed in [AlbumDetailActivity].
         */
        @BindsInstance
        abstract fun pickedAlbum(albums: MediaItem): Builder

        /**
         * Provides the color palette calculated from the album art of the picked album.
         */
        @BindsInstance
        abstract fun pickedAlbumPalette(palette: AlbumPalette): Builder

        override fun seedInstance(instance: AlbumDetailActivity) {
            val album = instance.intent?.getParcelableExtra<MediaItem>(AlbumDetailActivity.ARG_PICKED_ALBUM)
            checkNotNull(album) { "Calling activity must specify the album to display." }
            pickedAlbum(album)

            val palette = instance.intent?.getParcelableExtra<AlbumPalette>(
                AlbumDetailActivity.ARG_PALETTE)
            pickedAlbumPalette(palette ?: AlbumColorModule().providesDefaultAlbumPalette(instance))
        }
    }
}

@Module(subcomponents = [AlbumDetailSubcomponent::class])
abstract class AlbumDetailModule {
    @Binds @IntoMap
    @ClassKey(AlbumDetailActivity::class)
    abstract fun bindAlbumDetailActivityInjectorFactory(builder: AlbumDetailSubcomponent.Builder): AndroidInjector.Factory<*>

    @Binds @IntoMap
    @ViewModelKey(AlbumDetailViewModel::class)
    abstract fun bindAlbumDetailViewModel(viewModel: AlbumDetailViewModel): ViewModel
}