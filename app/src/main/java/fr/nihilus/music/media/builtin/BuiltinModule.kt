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
@Module(includes = arrayOf(HomeScreenModule::class))
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
    @StringKey(MediaID.ID_RANDOM)
    abstract fun bindRandom(impl: AllTracksRandom): BuiltinItem

    @Binds @IntoMap
    @StringKey(MediaID.ID_MOST_RECENT)
    abstract fun bindMostRecentTracks(impl: MostRecentTracks): BuiltinItem
}