package fr.nihilus.mymusic.playback;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import fr.nihilus.mymusic.HomeActivity;
import fr.nihilus.mymusic.utils.MediaIDHelper;

import static fr.nihilus.mymusic.utils.MediaIDHelper.MEDIA_ID_ALL_MUSIC;
import static fr.nihilus.mymusic.utils.MediaIDHelper.MEDIA_ID_ROOT;

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
    private static final String TAG = "MusicService";
    private static final long STOP_DELAY = 30000;
    private MediaSessionCompat mSession;
    private MusicProvider mMusicProvider;
    private List<QueueItem> mPlayingQueue;
    private Playback mPlayback;
    private boolean mServiceStarted;
    private int mCurrentIndexQueue;
    private DelayedStopHandler mDelayedStopHandler = new DelayedStopHandler(this);
    private MediaNotificationManager mMediaNotificationManager;

    @Override
    public void onCreate() {
        super.onCreate();

        mMusicProvider = new MusicProvider();
        mPlayingQueue = new LinkedList<>();

        mPlayback = new Playback(this, mMusicProvider);
        mPlayback.setState(PlaybackStateCompat.STATE_NONE);
        mPlayback.setCallback(this);


        // Création de la MediaSession
        ComponentName mediaButtonReceiver = new ComponentName(this, RemoteControlReceiver.class);
        mSession = new MediaSessionCompat(this, MusicService.class.getSimpleName(),
                mediaButtonReceiver, null);
        setSessionToken(mSession.getSessionToken());

        // Utile uniquement pour l'API < 21
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Crée un Intent permettant d'afficher une interface graphique liée à la MediaSession
        Context context = getApplicationContext();
        Intent intent = new Intent(context, HomeActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 99 /* Request code */,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mSession.setSessionActivity(pi);

        updatePlaybackState(null);

        mMediaNotificationManager = new MediaNotificationManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            String command = intent.getStringExtra(CMD_NAME);
            if (ACTION_CMD.equals(action)) {
                if (CMD_PAUSE.equals(command)) {
                    if (mPlayback != null && mPlayback.isPlaying()) {
                        handlePauseRequest();
                    }
                }
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

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid,
                                 @Nullable Bundle rootHints) {
        String thisApplicationPackage = getApplication().getPackageName();
        if (!clientPackageName.equals(thisApplicationPackage)) {
            Log.w(TAG, "onGetRoot: IGNORING request from untrusted package " + clientPackageName);
            return null;
        }
        return new BrowserRoot(MEDIA_ID_ROOT, null);
    }

    @Override
    public void onLoadChildren(@NonNull final String parentId,
                               @NonNull final Result<List<MediaItem>> result) {
        if (!mMusicProvider.isInitialized()) {
            Log.d(TAG, "onLoadChildren: must load music library before returning children.");
            result.detach();

            mMusicProvider.retrieveMetadataAsync(this, new MusicProvider.Callback() {
                @Override
                public void onMusicCatalogReady(boolean success) {
                    if (success) {
                        loadChildrenImpl(parentId, result);
                    } else {
                        Log.e(TAG, "onMusicCatalogReady: No MediaMetadata to load.");
                        updatePlaybackState("No MediaMetadata");
                        result.sendResult(Collections.<MediaItem>emptyList());
                    }
                }
            });
        } else {
            // Le catalogue est déjà chargé, on l'envoie immédiatement
            loadChildrenImpl(parentId, result);
        }
    }

    private void loadChildrenImpl(final String parentMediaId,
                                  final Result<List<MediaItem>> result) {
        List<MediaItem> mediaItems = new ArrayList<>();

        if (MEDIA_ID_ROOT.equals(parentMediaId)) {
            Log.d(TAG, "loadChildrenImpl: loading ROOT");
            mediaItems.add(new MediaItem(new MediaDescriptionCompat.Builder()
                    .setTitle("All tracks")
                    .setMediaId(MEDIA_ID_ALL_MUSIC)
                    .build(), MediaItem.FLAG_BROWSABLE));
        } else if (MEDIA_ID_ALL_MUSIC.equals(parentMediaId)) {
            Log.d(TAG, "loadChildrenImpl: loading ALL_MUSIC");
            for (MediaMetadataCompat track : mMusicProvider.getAllMusic()) {
                String hierarchyAwareMediaID = MediaIDHelper.createMediaID(track.getDescription()
                        .getMediaId(), MEDIA_ID_ALL_MUSIC, MEDIA_ID_ALL_MUSIC);
                MediaMetadataCompat trackCopy = new MediaMetadataCompat.Builder(track)
                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, hierarchyAwareMediaID)
                        .build();
                MediaItem item = new MediaItem(trackCopy.getDescription(), MediaItem.FLAG_PLAYABLE);
                mediaItems.add(item);
            }

            Collections.sort(mediaItems, new Comparator<MediaItem>() {
                @Override
                public int compare(MediaItem one, MediaItem another) {
                    String oneTitle = one.getDescription().getTitle().toString();
                    String anotherTitle = another.getDescription().getTitle().toString();
                    return oneTitle.compareTo(anotherTitle);
                }
            });
        }

        result.sendResult(mediaItems);
    }

    @Override
    public void onCompletion() {
        // On vient de finir la lecture d'une piste, on passe à la suivante
        if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
            mCurrentIndexQueue++;

            if (mCurrentIndexQueue >= mPlayingQueue.size()) {
                mCurrentIndexQueue = 0;
            }
            handlePlayRequest();
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

    /**
     * Demande la mise en pause de la lecture en cours.
     */
    void handlePauseRequest() {
        Log.d(TAG, "handlePauseRequest: mState=" + mPlayback.getState());
        mPlayback.pause();
        // Reset the delayed stop handler.
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
    }

    /**
     * Demande l'arrêt définitif du lecteur musical.
     *
     * @param withError message d'erreur optionnel, ou null s'il n'y a pas d'erreur
     */
    void handleStopRequest(@Nullable String withError) {
        Log.d(TAG, "handleStopRequest: mState=" + mPlayback.getState()
                + " error=" + withError);
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
    @SuppressWarnings("WrongConstant")
    private void updatePlaybackState(@Nullable String withError) {
        Log.d(TAG, "updatePlaybackState, playback state=" + mPlayback.getState());
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        if (mPlayback != null && mPlayback.isConnected()) {
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
    private long getAvailableActions() {
        long actions = PlaybackStateCompat.ACTION_PLAY
                | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
        if (mPlayingQueue == null || mPlayingQueue.isEmpty()) {
            return actions;
        }
        if (mPlayback.isPlaying()) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        }
        if (mCurrentIndexQueue > 0) {
            actions |= PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        }
        if (mCurrentIndexQueue < mPlayingQueue.size() - 1) {
            actions |= PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        }
        return actions;
    }

    private void setCustomActions(PlaybackStateCompat.Builder stateBuilder) {
        // TODO Actions : random and repeat
    }

    void handlePlayRequest() {
        Log.d(TAG, "handlePlayRequest: mState=" + mPlayback.getState());

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
        String musicId = MediaIDHelper.extractMusicIDFromMediaID(queueItem.getDescription().getMediaId());
        MediaMetadataCompat track = mMusicProvider.getMusic(musicId);
        final String trackId = track.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
        if (!musicId.equals(trackId)) {
            IllegalStateException e = new IllegalStateException("Track ID should match musicId.");
            Log.e(TAG, "updateMetadata: illegal state", e);
            throw e;
        }
        Log.d(TAG, "updateMetadata: musicId=" + musicId);
        mSession.setMetadata(track);

        // Récupère l'image de l'album pour l'afficher sur l'écran de verrouillage
        /*if(track.getDescription().getIconBitmap() == null
                && track.getDescription().getIconUri() != null) {
            Uri albumUri = track.getDescription().getIconUri();
            Glide.with(this).loadFromMediaStore(albumUri)
                    .asBitmap()
                    .thumbnail(0.4f)
                    .listener(new RequestListener<Uri, Bitmap>() {
                        @Override
                        public boolean onException(Exception e, Uri model, Target<Bitmap> target, boolean isFirstResource) {
                            Log.w(TAG, "updateMetadata: failed to load albumArt.", e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource, Uri model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
                            MediaSessionCompat.QueueItem queueItem = mPlayingQueue.get(mCurrentIndexQueue);
                            MediaMetadataCompat track = mMusicProvider.getMusic(trackId);
                            track = new MediaMetadataCompat.Builder(track)
                                    .putBitmap(METADATA_KEY_ALBUM_ART, resource)
                                    .putBitmap(METADATA_KEY_DISPLAY_ICON, resource)
                                    .build();
                            return false;
                        }
                    });
        }*/
    }

    void handlePreviousRequest() {
        mCurrentIndexQueue--;

        if (mPlayingQueue != null && mCurrentIndexQueue < 0) {
            // Si on revient avant le premier item, on le rejoue
            mCurrentIndexQueue = 0;
        }

        // Ne pas lancer la lecture si le lecteur est en pause
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
            // Met à jour les métadonnées de la chanson en cours de lecture
            updateMetadata();
        }
    }

    void handleNextRequest() {
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
     * Provoque l'arrêt automatique du service après {@link #STOP_DELAY} millisecondes.
     * Ce délai n'est effectif que lorsque le service n'est pas en train de lire de la musique.
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
                Log.d(TAG, "DelayedStopHandler: arrêt automatique du service");
                service.stopSelf();
                service.mServiceStarted = false;
            }
        }
    }

    /**
     * Callbacks envoyés au service par le MediaController pour transmettre les commandes.
     */
    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            Log.d(TAG, "onPlay");
            if (mPlayingQueue == null || mPlayingQueue.isEmpty()) {
                // TODO Spécialiser en fonction de la dernière lecture
                Log.d(TAG, "onPlay: generating random queue!");
                mPlayingQueue = QueueHelper.getAllMusic(mMusicProvider);
                mSession.setQueue(mPlayingQueue);
                mSession.setQueueTitle("Random");
                // On commence à lire du début
                mCurrentIndexQueue = 0;
            }

            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                handlePlayRequest();
            }
        }

        @Override
        public void onPause() {
            Log.d(TAG, "onPause: currentState=" + mPlayback.getState());
            handlePauseRequest();
        }

        @Override
        public void onStop() {
            Log.d(TAG, "onStop: currentState=" + mPlayback.getState());
            handleStopRequest(null);
        }

        @Override
        public void onSkipToQueueItem(long queueId) {
            Log.d(TAG, "onSkipToQueueItem: " + queueId);

            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                // Met à jour l'index de l'item joué actuellement
                mCurrentIndexQueue = QueueHelper.getMusicIndexOnQueue(mPlayingQueue, queueId);
                handlePlayRequest();
            }
        }

        @Override
        public void onSkipToNext() {
            Log.d(TAG, "onSkipToNext");
            handleNextRequest();
        }

        @Override
        public void onSkipToPrevious() {
            Log.d(TAG, "onSkipToPrevious");
            handlePreviousRequest();
        }

        @Override
        public void onSeekTo(long position) {
            Log.d(TAG, "onSeekTo: " + position);
            mPlayback.seekTo((int) position);
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            Log.d(TAG, "onPlayFromMediaId:" + "mediaId = [" + mediaId + "], " +
                    "extras = [" + extras + "]");

            mPlayingQueue = QueueHelper.getPlayingQueue(mediaId, mMusicProvider);
            mSession.setQueue(mPlayingQueue);
            mSession.setQueueTitle("All tracks");

            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                mCurrentIndexQueue = QueueHelper
                        .getMusicIndexOnQueue(mPlayingQueue, mediaId);
                if (mCurrentIndexQueue < 0) {
                    Log.e(TAG, "onPlayFromMediaId: media ID " + mediaId +
                            " not found in queue. Ignoring.");
                } else {
                    handlePlayRequest();
                }
            }
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {

        }
    }

    /**
     * Un BroadcastReceiver qui notifie le service lorsqu'un bouton média est pressé, par exemple
     * le bouton d'un casque filaire ou Bluetooth.
     */
    public class RemoteControlReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        handlePlayRequest();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                        handlePauseRequest();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        handleNextRequest();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        handlePreviousRequest();
                        break;
                }
            }

        }
    }
}
