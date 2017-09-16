package fr.nihilus.music.playback

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.support.v4.media.session.PlaybackStateCompat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.isEmptyOrNullString
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class ExoMusicPlayerTest {

    private var context = InstrumentationRegistry.getTargetContext()

    private lateinit var subject: ExoMusicPlayer

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        subject = ExoMusicPlayer(context)
    }

    @Test
    fun atStartup_stateIsNone() {
        assertThat(subject.state, `is`(PlaybackStateCompat.STATE_NONE))
    }

    @Test
    fun atStartup_playerIsNotPlaying() {
        assertThat(subject.isPlaying, `is`(false))
        assertThat(subject.currentPosition, `is`(0L))
        assertThat(subject.currentMediaId, isEmptyOrNullString())
    }

    @Test(expected = IllegalStateException::class)
    fun whenPlayingItem_failIfNoMediaId() {
        val itemToPlay = queueItemWith(mediaId = null)
        subject.play(itemToPlay)
    }

    @Test(expected = IllegalStateException::class)
    fun whenPlayingItem_failIfNoMediaUri() {
        val itemToPlay = queueItemWith(mediaId = "MUSIC|12", mediaUri = null)
        subject.play(itemToPlay)
    }
}