package fr.nihilus.mymusic.playback;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.RemoteException;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import fr.nihilus.mymusic.HomeActivity;
import fr.nihilus.mymusic.R;

/**
 * Une classe associée à {@link MusicService} permettant l'affichage d'une notification
 * pour contrôler le Playback.
 */
public class MediaNotificationManager extends BroadcastReceiver {

    private static final int NOTIFICATION_ID = 16;
    private static final int REQUEST_CODE = 100;
    private static final String ACTION_PAUSE = "fr.nihilus.nihilusmusic.PAUSE";
    private static final String ACTION_PLAY = "fr.nihilus.nihilusmusic.PLAY";
    private static final String ACTION_PREV = "fr.nihilus.nihilusmusic.PREV";
    private static final String ACTION_NEXT = "fr.nihilus.nihilusmusic.NEXT";
    private static final String TAG = "NotificationManager";
    private final MusicService mService;
    private final NotificationManager mNotificationManager;
    private final PendingIntent mPauseIntent;
    private final PendingIntent mPlayIntent;
    private final PendingIntent mPreviousIntent;
    private final PendingIntent mNextIntent;
    private MediaSessionCompat.Token mSessionToken;
    private MediaControllerCompat mController;
    private MediaControllerCompat.TransportControls mTransportControls;
    private PlaybackStateCompat mPlaybackState;
    private MediaMetadataCompat mMetadata;
    private boolean mStarted = false;
    private final MediaControllerCompat.Callback mCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            mPlaybackState = state;
            Log.d(TAG, "Received new playback state " + state);
            if (state != null && (state.getState() == PlaybackStateCompat.STATE_STOPPED
                    || state.getState() == PlaybackStateCompat.STATE_NONE)) {
                stopNotification();
            } else {
                Notification notification = createNotification();
                if (notification != null) {
                    mNotificationManager.notify(NOTIFICATION_ID, notification);
                }
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            mMetadata = metadata;
            Log.d(TAG, "Received new metadata " + metadata);
            Notification notification = createNotification();
            if (notification != null) {
                mNotificationManager.notify(NOTIFICATION_ID, notification);
            }
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
            Log.d(TAG, "Session was destroyed, resetting to the new session token");
            updateSessionToken();
        }
    };

    public MediaNotificationManager(MusicService service) {
        mService = service;
        updateSessionToken();

        mNotificationManager = (NotificationManager)
                mService.getSystemService(Context.NOTIFICATION_SERVICE);

        String pkg = mService.getPackageName();
        mPauseIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_PAUSE).setPackage(pkg),
                PendingIntent.FLAG_CANCEL_CURRENT);
        mPlayIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_PLAY).setPackage(pkg),
                PendingIntent.FLAG_CANCEL_CURRENT);
        mPreviousIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_PREV).setPackage(pkg),
                PendingIntent.FLAG_CANCEL_CURRENT);
        mNextIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_NEXT).setPackage(pkg),
                PendingIntent.FLAG_CANCEL_CURRENT);

        // Annule toutes les notifications pour gérer le cas où
        // le service serait arrêté par le système.
        mNotificationManager.cancelAll();
    }

    public void startNotification() {
        if (!mStarted) {
            mMetadata = mController.getMetadata();
            mPlaybackState = mController.getPlaybackState();

            // Mise à jour de la notification après avoir mis mStarted à true
            Notification notif = createNotification();
            if (notif != null) {
                mController.registerCallback(mCallback);
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_NEXT);
                filter.addAction(ACTION_PAUSE);
                filter.addAction(ACTION_PLAY);
                filter.addAction(ACTION_PREV);
                mService.registerReceiver(this, filter);

                mService.startForeground(NOTIFICATION_ID, notif);
                mStarted = true;
            }
        }
    }

    private void stopNotification() {
        if (mStarted) {
            mStarted = false;
            mController.unregisterCallback(mCallback);
            try {
                mNotificationManager.cancel(NOTIFICATION_ID);
                mService.unregisterReceiver(this);
            } catch (IllegalArgumentException ex) {
                // On ignore le cas où le Receiver n'est pas enregistré.
            }
            mService.stopForeground(true);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Log.d(TAG, "onReceive: intent with action=" + action);
        switch (action) {
            case ACTION_PAUSE:
                mTransportControls.pause();
                break;
            case ACTION_PLAY:
                mTransportControls.play();
                break;
            case ACTION_NEXT:
                mTransportControls.skipToNext();
                break;
            case ACTION_PREV:
                mTransportControls.skipToPrevious();
                break;
            default:
                Log.w(TAG, "Unknown intent ignored. Action=" + action);
        }
    }

    private void updateSessionToken() {
        MediaSessionCompat.Token freshToken = mService.getSessionToken();
        if (mSessionToken == null || !mSessionToken.equals(freshToken)) {
            if (mController != null) {
                mController.unregisterCallback(mCallback);
            }
            try {
                mSessionToken = freshToken;
                mController = new MediaControllerCompat(mService, mSessionToken);
                mTransportControls = mController.getTransportControls();
                if (mStarted) {
                    mController.registerCallback(mCallback);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "updateSessionToken: erreur à la création du MediaController.", e);
            }
        }
    }

    private PendingIntent createContentIntent() {
        Intent openUI = new Intent(mService, HomeActivity.class);
        openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(mService, REQUEST_CODE, openUI,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private Notification createNotification() {
        Log.d(TAG, "createNotification: mMetadata=" + mMetadata);
        if (mMetadata == null || mPlaybackState == null) {
            return null;
        }

        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(mService);
        int playPauseButtonPosition = 0;

        // Si l'action "Previous" est activée
        if ((mPlaybackState.getActions()
                & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0) {
            notifBuilder.addAction(R.drawable.ic_skip_previous_white_24dp,
                    "Previous", mPreviousIntent);
            // S'il y a un bouton "previous", alors "Play/Pause" est le second.
            // On garde ça en mémoire, parce que MediaStyle nécessite de préciser
            // l'index des boutons visibles dans le mode compact de la notification.
            playPauseButtonPosition = 1;
        }

        addPlayPauseButton(notifBuilder);

        // Si le bouton next est activé
        if ((mPlaybackState.getActions()
                & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) {
            notifBuilder.addAction(R.drawable.ic_skip_next_white_24dp,
                    "Next", mNextIntent);
        }

        MediaDescriptionCompat description = mMetadata.getDescription();
        Bitmap placeholder = BitmapFactory.decodeResource(mService.getResources(),
                R.drawable.dummy_album_art);

        notifBuilder.setStyle(new NotificationCompat.MediaStyle()
                // N'affiche que le play/pause en mode compact
                .setShowActionsInCompactView(playPauseButtonPosition)
                .setMediaSession(mSessionToken))
                .setColor(0x212121)
                .setSmallIcon(R.drawable.ic_audiotrack_white_24dp)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setUsesChronometer(true)
                .setContentIntent(createContentIntent())
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setLargeIcon(placeholder);

        setNotificationPlaybackState(notifBuilder);
        fetchAlbumArtAsync(description.getIconUri(), notifBuilder);

        return notifBuilder.build();
    }

    private void addPlayPauseButton(NotificationCompat.Builder builder) {
        Log.d(TAG, "updatePlayPauseAction");
        String label;
        @DrawableRes int icon;
        PendingIntent intent;
        if (mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            label = "Pause";
            icon = R.drawable.ic_pause_24dp;
            intent = mPauseIntent;
        } else {
            label = "Play";
            icon = R.drawable.ic_play_arrow_white_24dp;
            intent = mPlayIntent;
        }
        builder.addAction(new NotificationCompat.Action(icon, label, intent));
    }

    private void setNotificationPlaybackState(NotificationCompat.Builder builder) {
        Log.d(TAG, "setNotificationPlaybackState. mPlaybackState=" + mPlaybackState);
        if (mPlaybackState == null || !mStarted) {
            Log.d(TAG, "setNotificationPlaybackState: cancelling notification!");
            mService.stopForeground(true);
            return;
        }

        if (mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING
                && mPlaybackState.getPosition() >= 0) {
            Log.d(TAG, "setNotificationPlaybackState. updating playback position");
            builder.setWhen(System.currentTimeMillis() - mPlaybackState.getPosition())
                    .setShowWhen(true)
                    .setUsesChronometer(true);
        } else {
            Log.d(TAG, "setNotificationPlaybackState. hiding playback position");
            builder.setWhen(0)
                    .setShowWhen(false)
                    .setUsesChronometer(false);
        }

        // Permet de dégager la notification lorsque le player est en pause
        builder.setOngoing(mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING);
    }

    private void fetchAlbumArtAsync(final Uri artUri, final NotificationCompat.Builder builder) {
        Drawable placeholder = ContextCompat.getDrawable(mService, R.drawable.dummy_album_art);
        Glide.with(mService.getApplicationContext())
                .load(artUri).asBitmap()
                .error(placeholder)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap art, GlideAnimation<? super Bitmap> anim) {
                        // Si la pochette d'album est toujours celle du titre en cours de lecture
                        if (artUri.equals(mMetadata.getDescription().getIconUri())) {
                            Log.d(TAG, "fetchAlbumArtAsync: set bitmap to " + artUri);
                            builder.setLargeIcon(art);
                            mNotificationManager.notify(NOTIFICATION_ID, builder.build());
                        }
                    }
                });
    }
}
