package fr.nihilus.mymusic.service;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import fr.nihilus.mymusic.HomeActivity;
import fr.nihilus.mymusic.R;
import fr.nihilus.mymusic.provider.StatsRecorder;
import fr.nihilus.mymusic.settings.Prefs;
import fr.nihilus.mymusic.utils.MediaID;

import static fr.nihilus.mymusic.utils.MediaID.ID_ALBUMS;
import static fr.nihilus.mymusic.utils.MediaID.ID_ARTISTS;
import static fr.nihilus.mymusic.utils.MediaID.ID_DAILY;
import static fr.nihilus.mymusic.utils.MediaID.ID_MUSIC;
import static fr.nihilus.mymusic.utils.MediaID.ID_ROOT;

public class MusicService extends MediaBrowserServiceCompat implements Playback.Callback {

    /**
     * The action of the incoming Intent indicating that it contains a command
     * to be executed (see {@link #onStartCommand})
     */
    public static final String ACTION_CMD = "fr.nihilus.mymusic.ACTION_CMD";
    /**
     * The key in the extras of the incoming Intent indicating the command that
     * should be executed (see {@link #onStartCommand})
     */
    public static final String CMD_NAME = "CMD_NAME";
    /**
     * Valeur pour la clé de l'extra CMD_NAME issue de l'Intent indiquant
     * que la lecture musicale doit être mise en pause.
     *
     * @see #onStartCommand(Intent, int, int)
     */
    public static final String CMD_PAUSE = "CMD_PAUSE";
    public static final String CUSTOM_ACTION_RANDOM = "fr.nihilus.mymusic.ACTION_RANDOM";
    public static final long ACTION_RANDOM = 262144L;
    public static final String EXTRA_RANDOM_ENABLED = "random_enabled";
    public static final int REQUEST_HOME_ACTIVITY_PLAYER = 99;

    private static final String TAG = "MusicService";
    private static final long STOP_DELAY = 30000;
    private static final int SKIP_PREVIOUS_DELAY = 5000;
    public static final int SKIP_STAT_THRESHOLD = 5000;

    private final DelayedStopHandler mDelayedStopHandler = new DelayedStopHandler(this);
    private MediaSessionCompat mSession;
    private MusicProvider mMusicProvider;
    private List<QueueItem> mPlayingQueue;
    private Playback mPlayback;
    private boolean mServiceStarted;
    private int mCurrentIndexQueue;
    private MediaNotificationManager mMediaNotificationManager;
    private StatsRecorder mStatsRecorder;

    private boolean mRandomEnabled;

    @Override
    public void onCreate() {
        super.onCreate();

        mMusicProvider = new MusicProvider();
        mPlayingQueue = new LinkedList<>();

        // Create an Intent that will start MediaSession when receiving mediabutton events
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MusicService.class);
        PendingIntent mbrIntent = PendingIntent.getService(this, 0, mediaButtonIntent, 0);

        // Initiate MediaSession
        ComponentName mbrComponent = new ComponentName(this, MediaButtonReceiver.class);
        mSession = new MediaSessionCompat(this, MusicService.class.getSimpleName(),
                mbrComponent, mbrIntent);
        setSessionToken(mSession.getSessionToken());
        mSession.setCallback(new MediaSessionCallback());
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Restarts an inactive MediaSession with media button events in API 21+
        mSession.setMediaButtonReceiver(mbrIntent);

        // Associate an UI to this MediaSession
        Context context = getApplicationContext();
        Intent intent = new Intent(context, HomeActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, REQUEST_HOME_ACTIVITY_PLAYER,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mSession.setSessionActivity(pi);

        // Prepare playback
        mPlayback = new Playback(this);
        mPlayback.setState(PlaybackStateCompat.STATE_NONE);
        mPlayback.setCallback(this);
        mMediaNotificationManager = new MediaNotificationManager(this);

        // Start stats recorder
        mStatsRecorder = new StatsRecorder();

        mRandomEnabled = Prefs.isRandomPlayingEnabled(this);
        updatePlaybackState(null);
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid,
                                 @Nullable Bundle rootHints) {
        String thisApplicationPackage = getApplication().getPackageName();
        if (!clientPackageName.equals(thisApplicationPackage)) {
            Log.w(TAG, "onGetRoot: IGNORING request from untrusted package " + clientPackageName);
            return null;
        }
        return new BrowserRoot(ID_ROOT, null);
    }

    @Override
    public void onLoadChildren(@NonNull final String parentId,
                               @NonNull final Result<List<MediaItem>> result) {
        if (mMusicProvider.isNotInitialized()) {
            Log.v(TAG, "onLoadChildren: must load music library before returning children.");
            mMusicProvider.loadMetadata(this);
            buildFirstQueue();
        }

        Log.d(TAG, "onLoadChildren: parentId=" + parentId);

        String[] hierarchy = MediaID.getHierarchy(parentId);

        switch (hierarchy[0]) {
            case ID_MUSIC:
                result.sendResult(mMusicProvider.getMusicItems());
                break;
            case ID_ALBUMS:
                if (hierarchy.length > 1) {
                    result.sendResult(mMusicProvider.getAlbumTracksItems(parentId));
                } else {
                    result.sendResult(mMusicProvider.getAlbumItems(this));
                }
                break;
            case ID_ARTISTS:
                if (hierarchy.length > 1) {
                    Log.d(TAG, "onLoadChildren: loading detail of artist " + hierarchy[1]);
                    result.sendResult(mMusicProvider.getArtistChildren(this, hierarchy[1]));
                } else {
                    Log.d(TAG, "onLoadChildren: loading all artists");
                    result.sendResult(mMusicProvider.getArtistItems(this));
                }
                break;
            case ID_DAILY:
                MediaItem daily = mMusicProvider.getRandomMusicItem();
                result.sendResult(Collections.singletonList(daily));
                break;
            default:
                Log.w(TAG, "loadChildrenImpl: MediaId not implemented.");
                result.sendResult(Collections.<MediaItem>emptyList());
                break;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            String command = intent.getStringExtra(CMD_NAME);
            if (ACTION_CMD.equals(action)) {
                switch (command) {
                    case CMD_PAUSE:
                        if (mPlayback != null && mPlayback.isPlaying()) handlePauseRequest();
                        break;
                }
            } else {
                MediaButtonReceiver.handleIntent(mSession, intent);
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        handleStopRequest(null);
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mSession.release();
        super.onDestroy();
    }

    @Override
    public void onCompletion() {
        // Update stats of the completed track
        QueueItem completedItem = mPlayingQueue.get(mCurrentIndexQueue);
        String musicId = MediaID.extractMusicID(completedItem.getDescription().getMediaId());
        mStatsRecorder.recordCompletion(Long.parseLong(musicId));

        // On vient de finir la lecture d'une piste, on passe à la suivante
        if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
            mCurrentIndexQueue++;

            if (mCurrentIndexQueue >= mPlayingQueue.size()) {
                mCurrentIndexQueue = 0;
                handlePauseRequest();
            } else {
                handlePlayRequest();
            }
        } else {
            // S'il n'y a rien à jouer, on stoppe et libère les ressources
            handleStopRequest(null);
        }
    }

    @Override
    public void onPlaybackStatusChanged(int state) {
        updatePlaybackState(null);
    }

    @Override
    public void onError(String error) {
        updatePlaybackState(error);
    }

    private void handlePauseRequest() {
        mPlayback.pause();
        // Reset the delayed stop handler.
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
    }

    /**
     * Stops the service and the media playback.
     *
     * @param withError an optional error message that leads the service to stop
     */
    private void handleStopRequest(@Nullable String withError) {
        mPlayback.stop(true);
        // Reset the delayed stop handler.
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);

        updatePlaybackState(withError);

        // Service is no longer necessary. Will be started again if needed.
        stopSelf();
        mServiceStarted = false;
    }

    /**
     * Met à jour l'état du playback en précisant l'état courant, la position du curseur,
     * la piste en cours de lecture et les actions possibles.
     *
     * @param withError message d'erreur optionnel, ou null s'il n'y a pas d'erreur
     */
    private void updatePlaybackState(@Nullable String withError) {
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        if (mPlayback != null) {
            position = mPlayback.getCurrentStreamPosition();
        }

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(getAvailableActions());

        setCustomActions(stateBuilder);
        int state = mPlayback.getState();

        // Tient compte de s'il y a une erreur dans l'état du playback
        if (withError != null) {
            // Error states are really only supposed to be used for errors
            // that cause playback to stop unexpectedly and persist until the user
            // takes action to fix it.
            stateBuilder.setErrorMessage(withError);
            state = PlaybackStateCompat.STATE_ERROR;
        }
        stateBuilder.setState(state, position, 1.0f,
                SystemClock.elapsedRealtime());

        // Met à jour activeQueueItemId si la position de lecture est correcte
        if (QueueHelper.isIndexPlayable(mCurrentIndexQueue, mPlayingQueue)) {
            QueueItem item = mPlayingQueue.get(mCurrentIndexQueue);
            stateBuilder.setActiveQueueItemId(item.getQueueId());
        }

        mSession.setPlaybackState(stateBuilder.build());

        if (state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_PAUSED) {
            mMediaNotificationManager.startNotification();
        }
    }

    /**
     * Détermine les actions disponibles pour un MediaController distant.
     * A partir des flags renvoyés par cette méthode, on peut par exemple marquer un bouton "next"
     * de l'interface graphique comme actif ou inactif, en fonction de la présence d'un flag.
     *
     * @return flags représentant les actions disponibles
     */
    @PlaybackStateCompat.MediaKeyAction
    private long getAvailableActions() {
        @PlaybackStateCompat.MediaKeyAction
        long actions = PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
        if (mRandomEnabled) {
            //noinspection WrongConstant
            actions |= MusicService.ACTION_RANDOM;
        }
        if (mPlayingQueue == null || mPlayingQueue.isEmpty()) {
            return actions;
        }
        actions |= PlaybackStateCompat.ACTION_PLAY_PAUSE;
        if (mCurrentIndexQueue > 0) {
            actions |= PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        }
        if (mCurrentIndexQueue < mPlayingQueue.size() - 1) {
            actions |= PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        }
        return actions;
    }

    private void setCustomActions(PlaybackStateCompat.Builder stateBuilder) {
        stateBuilder.addCustomAction(CUSTOM_ACTION_RANDOM, getString(R.string.action_random),
                R.drawable.ic_shuffle_24dp);
    }

    private void handlePlayRequest() {
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        if (!mServiceStarted) {
            Log.v(TAG, "Starting service");
            /* Le service doit continuer à fonctionner même après que le MediaBrowser
             * ait été déconnecté. Pour cela, on le démarre également avec startService,
             * car MediaBrowserServiceCompat se base sur bindService.
             * Quand on a plus besoin de lire de la musique, on arrête le service avec stopSelf().
             */
            startService(new Intent(getApplicationContext(), MusicService.class));
            mServiceStarted = true;
        }

        if (!mSession.isActive()) {
            mSession.setActive(true);
        }

        if (QueueHelper.isIndexPlayable(mCurrentIndexQueue, mPlayingQueue)) {
            /* Si l'index fait partie de la file, remplacer les métadonnées de la lecture en cours
             * et jouer le titre avec Playback.
             */
            updateMetadata();
            mPlayback.play(mPlayingQueue.get(mCurrentIndexQueue));
        }
    }

    private void updateMetadata() {
        if (!QueueHelper.isIndexPlayable(mCurrentIndexQueue, mPlayingQueue)) {
            Log.e(TAG, "updateMetadata: can't retrieve current metadata.");
            updatePlaybackState("No metadata");
            return;
        }

        QueueItem queueItem = mPlayingQueue.get(mCurrentIndexQueue);
        String mediaId = queueItem.getDescription().getMediaId();
        String musicId = MediaID.extractMusicID(mediaId);
        MediaMetadataCompat track = mMusicProvider.getMusic(musicId);
        if (track != null) {
            final String trackId = track.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
            if (!TextUtils.equals(musicId, trackId)) {
                IllegalStateException e = new IllegalStateException("Track ID should match musicId.");
                Log.e(TAG, "updateMetadata: illegal state", e);
                throw e;
            }
            mSession.setMetadata(track);
            Prefs.setLastPlayedMediaId(this, mediaId);
        }

        // Récupère l'image de l'album pour l'afficher sur l'écran de verrouillage
        /*if (track.getDescription().getIconBitmap() == null
                && track.getDescription().getIconUri() != null) {
            Uri albumArtUri = track.getDescription().getIconUri();
            Glide.with(this).loadFromMediaStore(albumArtUri).asBitmap()
                    .thumbnail(0.4f)
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation anim) {

                        }
                    });
        }*/
    }

    private void handlePreviousRequest() {
        if (mPlayback.getCurrentStreamPosition() < SKIP_PREVIOUS_DELAY) {
            // Currently playing song has not been playing for more than 5 seconds.
            // We want to play the previous track.
            mCurrentIndexQueue--;

            if (mPlayingQueue != null && mCurrentIndexQueue < 0) {
                // If we went back too far, reset queue to the first item.
                mCurrentIndexQueue = 0;
            }

            // Do not start playing track if the playback is paused
            if (mPlayback.isPlaying()) {
                if (QueueHelper.isIndexPlayable(mCurrentIndexQueue, mPlayingQueue)) {
                    handlePlayRequest();
                } else {
                    Log.e(TAG, "onSkipToPrevious: impossible de revenir au précédent." +
                            " Précédent=" + mCurrentIndexQueue +
                            " longueur=" + (mPlayingQueue == null ? "null" : mPlayingQueue.size()));
                    handleStopRequest("Cannot skip");
                }
            } else {
                // Update metadata without starting playback
                updateMetadata();
            }
        } else {
            // If playing this song for more than 5 seconds, play from the start.
            mPlayback.seekTo(0);
        }
    }

    private void handleNextRequest() {
        if (mPlayback.isPlaying() && mPlayback.getCurrentStreamPosition() < SKIP_STAT_THRESHOLD) {
            // Update stats of the skipped track only if it playing.
            // Song must be skipped before the use has heard the first 5 seconds,
            // in order to be sure he does not want to lister to that song.
            QueueItem skippedItem = mPlayingQueue.get(mCurrentIndexQueue);
            String musicId = MediaID.extractMusicID(skippedItem.getDescription().getMediaId());
            mStatsRecorder.recordSkip(Long.parseLong(musicId));
        }

        mCurrentIndexQueue++;

        if (mPlayingQueue != null && mCurrentIndexQueue >= mPlayingQueue.size()) {
            // Si on passe au suivant après le dernier item, on revient au début
            mCurrentIndexQueue = 0;
        }

        // Ne pas lancer la lecture si le lecteur est en pause
        if (mPlayback.isPlaying()) {
            if (QueueHelper.isIndexPlayable(mCurrentIndexQueue, mPlayingQueue)) {
                handlePlayRequest();
            } else {
                Log.e(TAG, "onSkipToNext: impossible de passer au suivant." +
                        " Suivant=" + mCurrentIndexQueue +
                        " longueur=" + (mPlayingQueue == null ? "null" : mPlayingQueue.size()));
                handleStopRequest("Cannot skip");
            }
        } else {
            // Met à jour les métdadonnées de la chanson à lire
            updateMetadata();
        }
    }

    /**
     * Rebuild playing queue from the id of the song last played since the application closure.
     */
    private void buildFirstQueue() {
        final String lastPlayedId = Prefs.getLastPlayedMediaId(this);
        if (lastPlayedId != null && mPlayingQueue.isEmpty()) {
            mPlayingQueue = QueueHelper.getPlayingQueue(lastPlayedId, mMusicProvider);
            if (mRandomEnabled) {
                QueueHelper.shuffleQueue(mPlayingQueue);
            }
            mCurrentIndexQueue = QueueHelper.getMusicIndexOnQueue(mPlayingQueue, lastPlayedId);
            if (QueueHelper.isIndexPlayable(mCurrentIndexQueue, mPlayingQueue)) {
                String musicId = MediaID.extractMusicID(lastPlayedId);
                final MediaMetadataCompat track = mMusicProvider.getMusic(musicId);
                mSession.setMetadata(track);
                updatePlaybackState(null);
            }
        }
    }

    /**
     * Automatically stops the service after {@link #STOP_DELAY} milliseconds,
     * if it is not currently playing.
     */
    private static class DelayedStopHandler extends Handler {
        private final WeakReference<MusicService> mWeakReference;

        DelayedStopHandler(MusicService service) {
            mWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            MusicService service = mWeakReference.get();
            if (service != null && service.mPlayback != null) {
                if (service.mPlayback.isPlaying()) {
                    // On n'arrête pas le service tant qu'il est en train de lire de la musique
                    return;
                }

                Log.d(TAG, "DelayedStopHandler: publish stats before stopping service.");
                service.mStatsRecorder.publish(service);

                Log.d(TAG, "DelayedStopHandler: arrêt automatique du service");
                service.stopSelf();
                service.mServiceStarted = false;
            }
        }
    }

    /**
     * Callbacks that are received from the {@link MediaControllerCompat}
     * in order to deliver commands to this service.
     * This callback also handles {@link KeyEvent#KEYCODE_HEADSETHOOK} by itself to support
     * skipping to next when the button is pressed twice.
     */
    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        /**
         * Delay a single click on the headset button by this amount of time.
         * This is the time the user has to press the button a second time in order to trigger a
         * "skip to next" instead of the usual "play/pause".
         */
        private static final int HEADSET_CLICK_DELAY = 250;

        private final Handler mHeadsetButtonHandler = new Handler();
        private volatile int mHeadsetClickCount;

        /**
         * Action to be executed when a delay of {@link #HEADSET_CLICK_DELAY} has elapsed
         * since the user has clicked the headset button only once.
         */
        private final Runnable mHeadsetButtonRunnable = new Runnable() {
            @Override
            public void run() {
                if (mHeadsetClickCount == 1) {
                    // Single click: play if paused, or pause if playing.
                    if (!mPlayback.isPlaying()) onPlay();
                    else onPause();
                    mHeadsetClickCount = 0;
                }
            }
        };

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            if (mediaButtonEvent != null) {
                KeyEvent event = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (event.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK
                        && event.getAction() == KeyEvent.ACTION_DOWN) {
                    // Event is produced by a click on the headset button.
                    mHeadsetClickCount++;
                    if (mHeadsetClickCount > 1) {
                        // Double click: skip to next song.
                        onSkipToNext();
                        mHeadsetButtonHandler.removeCallbacks(mHeadsetButtonRunnable);
                        mHeadsetClickCount = 0;
                        return true;
                    } else {
                        // Single click: delay action to wait for a potential second click.
                        mHeadsetButtonHandler.postDelayed(mHeadsetButtonRunnable, HEADSET_CLICK_DELAY);
                        return true;
                    }
                }
            }
            return super.onMediaButtonEvent(mediaButtonEvent);
        }

        @Override
        public void onPlay() {
            Log.d(TAG, "onPlay");
            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                handlePlayRequest();
            }
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            mPlayingQueue = QueueHelper.getPlayingQueue(mediaId, mMusicProvider);
            if (mRandomEnabled) QueueHelper.shuffleQueue(mPlayingQueue);
            else QueueHelper.sortQueue(mPlayingQueue);
            mSession.setQueue(mPlayingQueue);

            mSession.setQueueTitle(getString(R.string.now_playing));

            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                mCurrentIndexQueue = QueueHelper.getMusicIndexOnQueue(mPlayingQueue, mediaId);
                if (mCurrentIndexQueue < 0) {
                    Log.w(TAG, "onPlayFromMediaId: can't find index on queue. Playing from start.");
                    mCurrentIndexQueue = 0;
                }
                handlePlayRequest();
            }
        }

        @Override
        public void onSkipToQueueItem(long queueId) {
            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                mCurrentIndexQueue = QueueHelper.getMusicIndexOnQueue(mPlayingQueue, queueId);
                handlePlayRequest();
            }
        }

        @Override
        public void onPause() {
            handlePauseRequest();
        }

        @Override
        public void onSkipToNext() {
            handleNextRequest();
        }

        @Override
        public void onSkipToPrevious() {
            handlePreviousRequest();
        }

        @Override
        public void onStop() {
            handleStopRequest(null);
        }

        @Override
        public void onSeekTo(long position) {
            mPlayback.seekTo((int) position);
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            if (CUSTOM_ACTION_RANDOM.equals(action)) {
                if (extras != null) {
                    mRandomEnabled = extras.getBoolean(EXTRA_RANDOM_ENABLED, false);
                    long currentQueueId = mPlayingQueue.get(mCurrentIndexQueue).getQueueId();
                    if (mRandomEnabled) {
                        QueueHelper.shuffleQueue(mPlayingQueue);
                    } else {
                        QueueHelper.sortQueue(mPlayingQueue);
                    }
                    mCurrentIndexQueue = QueueHelper.getMusicIndexOnQueue(mPlayingQueue, currentQueueId);
                    Prefs.setRandomPlayingEnabled(MusicService.this, mRandomEnabled);
                    Log.d(TAG, "onCustomAction: random read is enabled: " + mRandomEnabled);
                    updatePlaybackState(null);
                }
            }
        }
    }

    private class SongContentObserver extends ContentObserver {
        public SongContentObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.w(TAG, "onChange: called with no Uri parameter.");
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri != null) {
                mMusicProvider.notifySongChanged(getContentResolver(), uri);
            }
        }
    }
}
