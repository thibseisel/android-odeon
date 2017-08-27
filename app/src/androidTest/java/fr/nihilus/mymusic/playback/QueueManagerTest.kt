package fr.nihilus.mymusic.playback

import android.content.res.Resources
import android.support.test.runner.AndroidJUnit4
import android.support.v4.media.session.MediaSessionCompat
import fr.nihilus.mymusic.media.repo.CachedMusicRepository
import fr.nihilus.mymusic.service.MusicService
import org.hamcrest.Matchers.`is`
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class QueueManagerTest {

    @Mock private lateinit var mockService: MusicService
    @Mock private lateinit var mockRepo: CachedMusicRepository

    private lateinit var subject: QueueManager

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        configureMockResources()
        subject = QueueManager(mockService, mockRepo)
    }

    private fun configureMockResources() {
        val mockRes = mock(Resources::class.java)
        `when`(mockRes.getString(anyInt())).thenReturn("")
        `when`(mockRes.getString(anyInt(), any())).thenReturn("")
        `when`(mockService.resources).thenReturn(mockRes)
    }

    @Test
    fun settingQueue_hasCurrentMusic() {
        assertThat(subject.currentMusic, `is`(null as MediaSessionCompat.QueueItem?))

        val queue = queueOf("MUSIC|12", "MUSIC|42", "MUSIC|1245")
        subject.setCurrentQueue("All music", queue)

        assertThat(subject.currentMusic, `is`(queue.first()))
    }

    @Test
    fun sameBrowsingCategory() {
        val queue1 = queueOf("ALBUMS/6|1", "ALBUMS/6|2")
        val queue2 = queueOf("ALBUMS/7|1", "ALBUMS/7|3")

        subject.setCurrentQueue("Album N°6", queue1)

        assertTrue(subject.isSameBrowsingCategory(queue1[0].description.mediaId!!))
        assertFalse(subject.isSameBrowsingCategory(queue2[0].description.mediaId!!))
    }
}