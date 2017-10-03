package fr.nihilus.music.media

import dagger.Binds
import dagger.Module
import fr.nihilus.music.media.builtin.BuiltinModule
import fr.nihilus.music.media.cache.LruMusicCache
import fr.nihilus.music.media.cache.MusicCache
import fr.nihilus.music.media.repo.CachedMusicRepository
import fr.nihilus.music.media.repo.MusicRepository
import fr.nihilus.music.media.source.MediaStoreMusicDao
import fr.nihilus.music.media.source.MusicDao

/**
 * Define relations in the object graph for the "media" group of features.
 * Those classes are bound to the application context and are instantiated only once.
 *
 * Binds annotated methods are used by Dagger to know which implementation
 * should be injected when asking for an abstract type.
 */
@Module(includes = arrayOf(BuiltinModule::class))
abstract class MediaModule {

    @Binds
    internal abstract fun bindsMusicCache(cacheImpl: LruMusicCache): MusicCache

    @Binds
    internal abstract fun bindsMusicDao(daoImpl: MediaStoreMusicDao): MusicDao

    @Binds
    internal abstract fun bindsMusicRepository(repoImpl: CachedMusicRepository): MusicRepository
}
