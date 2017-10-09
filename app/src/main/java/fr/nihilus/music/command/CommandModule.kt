package fr.nihilus.music.command

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

@Module
abstract class CommandModule {

    @Binds
    @IntoMap
    @StringKey(NewPlaylistCommand.CMD_NAME)
    abstract fun bindNewPlaylistCommand(cmd: NewPlaylistCommand): MediaSessionCommand

    @Binds
    @IntoMap
    @StringKey(DeletePlaylistCommand.CMD_NAME)
    abstract fun bindDeletePlaylistCommand(cmd: DeletePlaylistCommand): MediaSessionCommand

    @Binds
    @IntoMap
    @StringKey(DeleteTracksCommand.CMD_NAME)
    abstract fun bindDeleteTracksCommand(cmd: DeleteTracksCommand): MediaSessionCommand
}