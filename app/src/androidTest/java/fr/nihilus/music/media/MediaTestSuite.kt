package fr.nihilus.music.media

import fr.nihilus.music.media.datasource.MediaStoreMusicDaoTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * A Test Suite that runs all tests for classes in the [fr.nihilus.music.media] package.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(MediaStoreMusicDaoTest::class, CachedMusicRepositoryTest::class)
class MediaTestSuite