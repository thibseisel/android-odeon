package fr.nihilus.music.media.builtin

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import fr.nihilus.music.utils.MediaID

@Module
abstract class BuiltinModule {

    @Binds @IntoMap
    @StringKey(MediaID.ID_MOST_RECENT)
    abstract fun bindMostRecentTracks(builtIn: MostRecentTracks): BuiltinItem
}