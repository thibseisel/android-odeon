package fr.nihilus.mymusic.media;

import dagger.Binds;
import dagger.Module;

@Module
public abstract class MediaModule {

    @Binds
    abstract MusicCache bindsMusicCache(LruMusicCache cacheImpl);

    @Binds
    abstract MusicDao bindsMusicDao(MediaStoreMusicDao daoImpl);

    @Binds
    abstract MusicRepository bindsMusicRepository(CachedMusicRepository repositoryImpl);
}
