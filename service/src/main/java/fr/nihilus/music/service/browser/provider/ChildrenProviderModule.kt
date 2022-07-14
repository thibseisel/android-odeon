/*
 * Copyright 2022 Thibault Seisel
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

package fr.nihilus.music.service.browser.provider

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import javax.inject.Named

@Module
@InstallIn(ServiceComponent::class)
internal abstract class ChildrenProviderModule {

    @Binds @Named("tracks")
    abstract fun bindsTrackProvider(tracks: TrackChildrenProvider): ChildrenProvider

    @Binds @Named("albums")
    abstract fun bindsAlbumProvider(albums: AlbumChildrenProvider): ChildrenProvider

    @Binds @Named("artists")
    abstract fun bindsArtistProvider(artists: ArtistChildrenProvider): ChildrenProvider

    @Binds @Named("playlists")
    abstract fun bindsPlaylistProvider(playlists: PlaylistChildrenProvider): ChildrenProvider
}
