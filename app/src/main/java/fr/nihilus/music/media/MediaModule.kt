package fr.nihilus.music.media

import dagger.Binds
import dagger.Module
import fr.nihilus.music.media.builtin.BuiltinModule
import fr.nihilus.music.media.cache.LruMemoryCache
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
@Suppress("unused")
@Module(includes = arrayOf(BuiltinModule::class))
internal abstract class MediaModule {

    @Binds
    abstract fun bindsMusicCache(cacheImpl: LruMemoryCache): MusicCache

    @Binds
    abstract fun bindsMusicDao(daoImpl: MediaStoreMusicDao): MusicDao

    @Binds
    abstract fun bindsMusicRepository(repoImpl: CachedMusicRepository): MusicRepository
}
