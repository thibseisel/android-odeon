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

package fr.nihilus.music.media.builtin

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import fr.nihilus.music.utils.MediaID

/**
 * A Dagger module that groups implementations of the [BuiltinItem] interface in a [Map]
 * whose keys are the root media id of the items it provides.
 * This allow getting the appropriate items for a given media id in a more extensible way.
 *
 * To add a new category of items that can be fetched, create a new implementation
 * of a [BuiltinItem] and binds it to the map with the root media id it represents as the key.
 */
@Suppress("unused")
@Module
internal abstract class BuiltinModule {

    @Binds @IntoMap
    @StringKey(MediaID.ID_MUSIC)
    abstract fun bindAllTracks(impl: AllTracks): BuiltinItem

    @Binds @IntoMap
    @StringKey(MediaID.ID_ALBUMS)
    abstract fun bindAlbums(impl: AlbumItems): BuiltinItem

    @Binds @IntoMap
    @StringKey(MediaID.ID_ARTISTS)
    abstract fun bindArtists(impl: ArtistItems): BuiltinItem

    @Binds @IntoMap
    @StringKey(MediaID.ID_PLAYLISTS)
    abstract fun bindPlaylists(impl: PlaylistItems): BuiltinItem

    @Binds @IntoMap
    @StringKey(MediaID.ID_MOST_RECENT)
    abstract fun bindMostRecentTracks(impl: MostRecentTracks): BuiltinItem
}