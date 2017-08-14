package fr.nihilus.mymusic.playback

import fr.nihilus.mymusic.di.MusicServiceScope
import fr.nihilus.mymusic.media.MusicRepository
import fr.nihilus.mymusic.service.MusicService
import javax.inject.Inject

@MusicServiceScope
class QueueManager
@Inject constructor(
        service: MusicService,
        repository: MusicRepository
) {

}