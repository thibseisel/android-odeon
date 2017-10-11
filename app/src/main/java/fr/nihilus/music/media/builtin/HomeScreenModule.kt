package fr.nihilus.music.media.builtin

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet

@Module
abstract class AutomaticPlaylistsModule {

    @Binds @IntoSet
    abstract fun bindMostRecents(playlist: MostRecentTracks): BuiltinItem
}