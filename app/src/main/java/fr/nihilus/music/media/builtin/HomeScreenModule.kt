package fr.nihilus.music.media.builtin

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet

/**
 * A Dagger module that binds built-in items of the music library into a set.
 *
 * All items placed into the set should be displayed together as part of the main screen of the UI.
 */
@Suppress("unused")
@Module
internal abstract class HomeScreenModule {

    @Binds @IntoSet
    abstract fun bindMostRecents(playlist: MostRecentTracks): BuiltinItem
}