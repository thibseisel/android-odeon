package fr.nihilus.mymusic

import fr.nihilus.mymusic.di.MusicServiceScope
import javax.inject.Inject

@MusicServiceScope
internal class MediaNotificationManager
@Inject constructor(service: MusicService) {

    private val mService = service

    fun startNotification() {
        TODO("Implement notification construction and replacement")
    }
}