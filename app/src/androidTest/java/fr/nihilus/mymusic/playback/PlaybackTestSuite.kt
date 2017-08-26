package fr.nihilus.mymusic.playback

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(ExoPlaybackTest::class, QueueManagerTest::class)
class PlaybackTestSuite