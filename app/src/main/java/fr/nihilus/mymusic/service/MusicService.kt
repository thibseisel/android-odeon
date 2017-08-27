package fr.nihilus.mymusic.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import dagger.android.AndroidInjection
import fr.nihilus.mymusic.HomeActivity
import fr.nihilus.mymusic.media.repo.MusicRepository
import fr.nihilus.mymusic.playback.PlaybackManager
import fr.nihilus.mymusic.playback.QueueManager
import fr.nihilus.mymusic.utils.MediaID
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.lang.ref.WeakReference
import javax.inject.Inject

private const val TAG = "MusicService"

/** Number of milliseconds to wait until the service stops itself when not playing. */
private const val STOP_DELAY = 30000L

class MusicService : MediaBrowserServiceCompat(),
        PlaybackManager.ServiceCallback, QueueManager.MetadataUpdateListener {

    @Inject lateinit var mRepository: MusicRepository
    @Inject lateinit var mPlaybackManager: PlaybackManager
    @Inject lateinit var mNotificationManager: MediaNotificationManager

    private lateinit var mSession: MediaSessionCompat
    private val mDelayedStopHandler = DelayedStopHandler(this)

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()

        mSession = MediaSessionCompat(this, "MusicService")
        sessionToken = mSession.sessionToken
        mSession.setCallback(mPlaybackManager.mediaSessionCallback)
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

        val appContext = applicationContext
        val uiIntent = Intent(appContext, HomeActivity::class.java)
        val pi = PendingIntent.getActivity(appContext, 99,
                uiIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        mSession.setSessionActivity(pi)

        mPlaybackManager.init()
        mPlaybackManager.updatePlaybackState(null)

        mNotificationManager.init()
    }

    override fun onDestroy() {
        mPlaybackManager.handleStopRequest(null)
        mNotificationManager.stopNotification()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            if (ACTION_CMD == intent.action) {
                val cmd = intent.getStringExtra(CMD_NAME)
                if (CMD_PAUSE == cmd) {
                    mPlaybackManager.handlePauseRequest()
                }
            }
        }

        return START_STICKY
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        val thisApplicationPackage = application.packageName
        if (clientPackageName != thisApplicationPackage) {
            Log.w(TAG, "onGetRoot: IGNORING request from untrusted package $clientPackageName")
            return null
        }

        return BrowserRoot(MediaID.ID_ROOT, null)
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        Log.d(TAG, "Loading children for ID: $parentId")
        result.detach()
        mRepository.getMediaItems(parentId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<List<MediaItem>> {
                    override fun onSubscribe(d: Disposable) {}
                    override fun onError(e: Throwable) {
                        Log.e(TAG, "An error occured: $e")
                        result.sendResult(null)
                    }
                    override fun onSuccess(items: List<MediaItem>) {
                        Log.d(TAG, "Loaded items for $parentId: ${items.size}")
                        result.sendResult(items)
                    }
                })
    }

    override fun onPlaybackStart() {
        mSession.isActive = true
        mDelayedStopHandler.removeCallbacksAndMessages(null)

        /* The service must continue running aven after the bound client (usually a MediaController)
         * disconnects, otherwise the music playback will stop.
         * Calling startService(Intent) will keep the service running until it is explicitely killed.
         */
        startService(Intent(applicationContext, MusicService::class.java))
    }

    override fun onPlaybackStop() {
        mSession.isActive = false
        mDelayedStopHandler.removeCallbacksAndMessages(null)
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY)
        stopForeground(true)
    }

    override fun onShuffleModeChanged(@PlaybackStateCompat.ShuffleMode shuffleMode: Int) {
        mSession.setShuffleMode(shuffleMode)
    }

    override fun onNotificationRequired() {
        mNotificationManager.startNotification()
    }

    override fun onPlaybackStateUpdated(newState: PlaybackStateCompat) {
        mSession.setPlaybackState(newState)
    }

    override fun onMetadataChanged(metadata: MediaMetadataCompat) {
        mSession.setMetadata(metadata)
    }

    override fun onMetadataRetrieveError() {
        mPlaybackManager.updatePlaybackState("No metadata")
    }

    override fun onCurrentQueueIndexUpdated(queueIndex: Int) {
        mPlaybackManager.handlePlayRequest()
    }

    override fun onQueueUpdated(title: String, newQueue: List<MediaSessionCompat.QueueItem>) {
        mSession.setQueueTitle(title)
        mSession.setQueue(newQueue)
    }

    companion object {
        const val ACTION_CMD = "fr.nihilus.music.ACTION_CMD"
        const val CMD_NAME = "CMD_NAME"
        const val CMD_PAUSE = "CMD_PAUSE"
    }
}

private class DelayedStopHandler(service: MusicService) : Handler() {
    private val mServiceRef = WeakReference(service)

    override fun handleMessage(msg: Message?) {
        mServiceRef.get()?.let {
            Log.d(TAG, "Stopping service xith delay handler")
            it.stopSelf()
        }
    }
}
