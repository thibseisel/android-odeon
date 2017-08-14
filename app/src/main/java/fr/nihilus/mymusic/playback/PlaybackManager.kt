package fr.nihilus.mymusic.playback

import fr.nihilus.mymusic.di.MusicServiceScope
import fr.nihilus.mymusic.service.MusicService
import javax.inject.Inject

@MusicServiceScope
class PlaybackManager
@Inject constructor(
        service: MusicService,
        queueManager: QueueManager
) : LocalPlayback.Callback {

    override fun onCompletion() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onError(message: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPlaybackStatusChanged(newState: Int) {
        TODO("not implemented")
    }
}