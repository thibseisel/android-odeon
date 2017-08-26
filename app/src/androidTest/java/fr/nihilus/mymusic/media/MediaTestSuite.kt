package fr.nihilus.mymusic.media

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * A Test Suite that runs all tests for classes in the [fr.nihilus.mymusic.media] package.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(LocalMusicDaoTest::class, MusicRepositoryTest::class)
class MediaTestSuite