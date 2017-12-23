/*
 * Copyright (C) 2017 The Android Open Source Project
 * Modifications Copyright (C) 2017 Thibault Seisel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.nihilus.music.service;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Pair;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.ext.mediasession.DefaultPlaybackController;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector.CommandReceiver;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector.CustomActionProvider;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector.PlaybackController;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector.PlaybackPreparer;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector.QueueEditor;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector.QueueNavigator;
import com.google.android.exoplayer2.util.ErrorMessageProvider;
import com.google.android.exoplayer2.util.RepeatModeUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A reworked version of ExoPlayer's {@link MediaSessionConnector} that allow clients to override
 * the default way of updating media session metadata.
 *
 * @version ExoPlayer 2.6.0
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class MediaSessionController {

    /**
     * Handles updating the media session metadata.
     */
    public interface SessionMetadataUpdater {
        /**
         * Called when the metadata for this media session should be updated
         * with the currently playing track.
         *
         * @param session The media session to update.
         * @param player The player associated with the media session.
         */
        void onUpdateMediaSessionMetadata(@NonNull MediaSessionCompat session, @Nullable Player player);
    }

    /**
     * The default repeat toggle modes which is the bitmask of
     * {@link RepeatModeUtil#REPEAT_TOGGLE_MODE_ONE} and
     * {@link RepeatModeUtil#REPEAT_TOGGLE_MODE_ALL}.
     */
    public static final @RepeatModeUtil.RepeatToggleModes
    int DEFAULT_REPEAT_TOGGLE_MODES =
            RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE | RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL;
    public static final String EXTRAS_PITCH = "EXO_PITCH";
    private static final int BASE_MEDIA_SESSION_FLAGS = MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
            | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS;
    private static final int EDITOR_MEDIA_SESSION_FLAGS = BASE_MEDIA_SESSION_FLAGS
            | MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS;

    /**
     * The wrapped {@link MediaSessionCompat}.
     */
    public final MediaSessionCompat mediaSession;

    private final Handler handler;
    private final boolean doMaintainMetadata;
    private final ExoPlayerEventListener exoPlayerEventListener;
    private final MediaSessionCallback mediaSessionCallback;
    private final PlaybackController playbackController;
    private final Map<String, CommandReceiver> commandMap;

    private Player player;
    private CustomActionProvider[] customActionProviders;
    private Map<String, CustomActionProvider> customActionMap;
    private ErrorMessageProvider<? super ExoPlaybackException> errorMessageProvider;
    private PlaybackPreparer playbackPreparer;
    private QueueNavigator queueNavigator;
    private QueueEditor queueEditor;
    private ExoPlaybackException playbackException;
    private SessionMetadataUpdater metadataUpdater;

    /**
     * @see MediaSessionConnector#MediaSessionConnector(MediaSessionCompat)
     */
    public MediaSessionController(MediaSessionCompat mediaSession) {
        this(mediaSession, null);
    }

    /**
     * @see MediaSessionConnector#MediaSessionConnector(MediaSessionCompat, PlaybackController)
     */
    public MediaSessionController(MediaSessionCompat mediaSession,
                                 PlaybackController playbackController) {
        this(mediaSession, playbackController, true);
    }

    /**
     * @see MediaSessionConnector#MediaSessionConnector(MediaSessionCompat, PlaybackController, boolean)
     */
    public MediaSessionController(MediaSessionCompat mediaSession,
                                  PlaybackController playbackController,
                                  boolean doMaintainMetadata) {
        this.mediaSession = mediaSession;
        this.playbackController = playbackController != null ? playbackController
                : new DefaultPlaybackController();
        this.handler = new Handler(Looper.myLooper() != null ? Looper.myLooper()
                : Looper.getMainLooper());
        this.doMaintainMetadata = doMaintainMetadata;
        mediaSession.setFlags(BASE_MEDIA_SESSION_FLAGS);
        mediaSessionCallback = new MediaSessionCallback();
        exoPlayerEventListener = new ExoPlayerEventListener();
        customActionMap = Collections.emptyMap();
        commandMap = new HashMap<>();
        registerCommandReceiver(playbackController);
    }

    /**
     * @see MediaSessionConnector#setPlayer(Player, PlaybackPreparer, CustomActionProvider...)
     */
    public void setPlayer(Player player, PlaybackPreparer playbackPreparer,
                          CustomActionProvider... customActionProviders) {
        if (this.player != null) {
            this.player.removeListener(exoPlayerEventListener);
            mediaSession.setCallback(null);
        }
        unregisterCommandReceiver(this.playbackPreparer);

        this.player = player;
        this.playbackPreparer = playbackPreparer;
        registerCommandReceiver(playbackPreparer);

        this.customActionProviders = (player != null && customActionProviders != null)
                ? customActionProviders : new CustomActionProvider[0];
        if (player != null) {
            mediaSession.setCallback(mediaSessionCallback, handler);
            player.addListener(exoPlayerEventListener);
        }
        updateMediaSessionPlaybackState();
        updateMediaSessionMetadata();
    }

    /**
     * @see MediaSessionConnector#setErrorMessageProvider(ErrorMessageProvider)
     */
    public void setErrorMessageProvider(
            ErrorMessageProvider<? super ExoPlaybackException> errorMessageProvider) {
        this.errorMessageProvider = errorMessageProvider;
    }

    /**
     * @see MediaSessionConnector#setQueueNavigator(QueueNavigator)
     */
    public void setQueueNavigator(QueueNavigator queueNavigator) {
        unregisterCommandReceiver(this.queueNavigator);
        this.queueNavigator = queueNavigator;
        registerCommandReceiver(queueNavigator);
    }

    /**
     * @see MediaSessionConnector#setQueueEditor(QueueEditor)
     */
    public void setQueueEditor(QueueEditor queueEditor) {
        unregisterCommandReceiver(this.queueEditor);
        this.queueEditor = queueEditor;
        registerCommandReceiver(queueEditor);
        mediaSession.setFlags(queueEditor == null ? BASE_MEDIA_SESSION_FLAGS
                : EDITOR_MEDIA_SESSION_FLAGS);
    }

    /**
     * Sets the {@link SessionMetadataUpdater} to handle this session metadata updates.
     * If this is not set, metadata will not be updated.
     * @param updater The metadata updater.
     */
    public void setMetadataUpdater(SessionMetadataUpdater updater) {
        this.metadataUpdater = updater;
    }

    private void registerCommandReceiver(CommandReceiver commandReceiver) {
        if (commandReceiver != null && commandReceiver.getCommands() != null) {
            for (String command : commandReceiver.getCommands()) {
                commandMap.put(command, commandReceiver);
            }
        }
    }

    private void unregisterCommandReceiver(CommandReceiver commandReceiver) {
        if (commandReceiver != null && commandReceiver.getCommands() != null) {
            for (String command : commandReceiver.getCommands()) {
                commandMap.remove(command);
            }
        }
    }

    private void updateMediaSessionPlaybackState() {
        PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder();
        if (player == null) {
            builder.setActions(buildPlaybackActions()).setState(PlaybackStateCompat.STATE_NONE, 0, 0, 0);
            mediaSession.setPlaybackState(builder.build());
            return;
        }

        Map<String, CustomActionProvider> currentActions = new HashMap<>();
        for (CustomActionProvider customActionProvider : customActionProviders) {
            PlaybackStateCompat.CustomAction customAction = customActionProvider.getCustomAction();
            if (customAction != null) {
                currentActions.put(customAction.getAction(), customActionProvider);
                builder.addCustomAction(customAction);
            }
        }
        customActionMap = Collections.unmodifiableMap(currentActions);

        int sessionPlaybackState = playbackException != null ? PlaybackStateCompat.STATE_ERROR
                : mapPlaybackState(player.getPlaybackState(), player.getPlayWhenReady());
        if (playbackException != null) {
            if (errorMessageProvider != null) {
                Pair<Integer, String> message = errorMessageProvider.getErrorMessage(playbackException);
                builder.setErrorMessage(message.first, message.second);
            }
            if (player.getPlaybackState() != Player.STATE_IDLE) {
                playbackException = null;
            }
        }
        long activeQueueItemId = queueNavigator != null ? queueNavigator.getActiveQueueItemId(player)
                : MediaSessionCompat.QueueItem.UNKNOWN_ID;
        Bundle extras = new Bundle();
        extras.putFloat(EXTRAS_PITCH, player.getPlaybackParameters().pitch);
        builder.setActions(buildPlaybackActions())
                .setActiveQueueItemId(activeQueueItemId)
                .setBufferedPosition(player.getBufferedPosition())
                .setState(sessionPlaybackState, player.getCurrentPosition(),
                        player.getPlaybackParameters().speed, SystemClock.elapsedRealtime())
                .setExtras(extras);
        mediaSession.setPlaybackState(builder.build());
    }

    private long buildPlaybackActions() {
        long actions = (PlaybackController.ACTIONS
                & playbackController.getSupportedPlaybackActions(player));
        if (playbackPreparer != null) {
            actions |= (PlaybackPreparer.ACTIONS & playbackPreparer.getSupportedPrepareActions());
        }
        if (queueNavigator != null) {
            actions |= (QueueNavigator.ACTIONS &
                    queueNavigator.getSupportedQueueNavigatorActions(player));
        }
        if (queueEditor != null) {
            actions |= (QueueEditor.ACTIONS & queueEditor.getSupportedQueueEditorActions(player));
        }
        return actions;
    }

    private void updateMediaSessionMetadata() {
        if (doMaintainMetadata && metadataUpdater != null) {
            metadataUpdater.onUpdateMediaSessionMetadata(mediaSession, player);
        }
    }

    private int mapPlaybackState(int exoPlayerPlaybackState, boolean playWhenReady) {
        switch (exoPlayerPlaybackState) {
            case Player.STATE_BUFFERING:
                return PlaybackStateCompat.STATE_BUFFERING;
            case Player.STATE_READY:
                return playWhenReady ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
            case Player.STATE_ENDED:
                return PlaybackStateCompat.STATE_PAUSED;
            default:
                return PlaybackStateCompat.STATE_NONE;
        }
    }

    private boolean canDispatchToPlaybackPreparer(long action) {
        return playbackPreparer != null && (playbackPreparer.getSupportedPrepareActions()
                & PlaybackPreparer.ACTIONS & action) != 0;
    }

    private boolean canDispatchToPlaybackController(long action) {
        return (playbackController.getSupportedPlaybackActions(player)
                & PlaybackController.ACTIONS & action) != 0;
    }

    private boolean canDispatchToQueueNavigator(long action) {
        return queueNavigator != null && (queueNavigator.getSupportedQueueNavigatorActions(player)
                & QueueNavigator.ACTIONS & action) != 0;
    }

    private boolean canDispatchToQueueEditor(long action) {
        return queueEditor != null && (queueEditor.getSupportedQueueEditorActions(player)
                & QueueEditor.ACTIONS & action) != 0;
    }

    private class ExoPlayerEventListener extends Player.DefaultEventListener {

        private int currentWindowIndex;
        private int currentWindowCount;

        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {
            int windowCount = player.getCurrentTimeline().getWindowCount();
            int windowIndex = player.getCurrentWindowIndex();
            if (queueNavigator != null) {
                queueNavigator.onTimelineChanged(player);
                updateMediaSessionPlaybackState();
            } else if (currentWindowCount != windowCount || currentWindowIndex != windowIndex) {
                // active queue item and queue navigation actions may need to be updated
                updateMediaSessionPlaybackState();
            }
            if (currentWindowCount != windowCount) {
                // active queue item and queue navigation actions may need to be updated
                updateMediaSessionPlaybackState();
            }
            currentWindowCount = windowCount;
            currentWindowIndex = player.getCurrentWindowIndex();
            updateMediaSessionMetadata();
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            updateMediaSessionPlaybackState();
        }

        @Override
        public void onRepeatModeChanged(@Player.RepeatMode int repeatMode) {
            mediaSession.setRepeatMode(repeatMode == Player.REPEAT_MODE_ONE
                    ? PlaybackStateCompat.REPEAT_MODE_ONE : repeatMode == Player.REPEAT_MODE_ALL
                    ? PlaybackStateCompat.REPEAT_MODE_ALL : PlaybackStateCompat.REPEAT_MODE_NONE);
            updateMediaSessionPlaybackState();
        }

        @Override
        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
            mediaSession.setShuffleMode(shuffleModeEnabled ? PlaybackStateCompat.SHUFFLE_MODE_ALL
                    : PlaybackStateCompat.SHUFFLE_MODE_NONE);
            updateMediaSessionPlaybackState();
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            playbackException = error;
            updateMediaSessionPlaybackState();
        }

        @Override
        public void onPositionDiscontinuity(@Player.DiscontinuityReason int reason) {
            if (currentWindowIndex != player.getCurrentWindowIndex()) {
                if (queueNavigator != null) {
                    queueNavigator.onCurrentWindowIndexChanged(player);
                }
                currentWindowIndex = player.getCurrentWindowIndex();
                updateMediaSessionMetadata();
            }
            updateMediaSessionPlaybackState();
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            updateMediaSessionPlaybackState();
        }

    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {

        @Override
        public void onPlay() {
            if (canDispatchToPlaybackController(PlaybackStateCompat.ACTION_PLAY)) {
                playbackController.onPlay(player);
            }
        }

        @Override
        public void onPause() {
            if (canDispatchToPlaybackController(PlaybackStateCompat.ACTION_PAUSE)) {
                playbackController.onPause(player);
            }
        }

        @Override
        public void onSeekTo(long position) {
            if (canDispatchToPlaybackController(PlaybackStateCompat.ACTION_SEEK_TO)) {
                playbackController.onSeekTo(player, position);
            }
        }

        @Override
        public void onFastForward() {
            if (canDispatchToPlaybackController(PlaybackStateCompat.ACTION_FAST_FORWARD)) {
                playbackController.onFastForward(player);
            }
        }

        @Override
        public void onRewind() {
            if (canDispatchToPlaybackController(PlaybackStateCompat.ACTION_REWIND)) {
                playbackController.onRewind(player);
            }
        }

        @Override
        public void onStop() {
            if (canDispatchToPlaybackController(PlaybackStateCompat.ACTION_STOP)) {
                playbackController.onStop(player);
            }
        }

        @Override
        public void onSetShuffleMode(int shuffleMode) {
            if (canDispatchToPlaybackController(PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE)) {
                playbackController.onSetShuffleMode(player, shuffleMode);
            }
        }

        @Override
        public void onSetRepeatMode(int repeatMode) {
            if (canDispatchToPlaybackController(PlaybackStateCompat.ACTION_SET_REPEAT_MODE)) {
                playbackController.onSetRepeatMode(player, repeatMode);
            }
        }

        @Override
        public void onSkipToNext() {
            if (canDispatchToQueueNavigator(PlaybackStateCompat.ACTION_SKIP_TO_NEXT)) {
                queueNavigator.onSkipToNext(player);
            }
        }

        @Override
        public void onSkipToPrevious() {
            if (canDispatchToQueueNavigator(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)) {
                queueNavigator.onSkipToPrevious(player);
            }
        }

        @Override
        public void onSkipToQueueItem(long id) {
            if (canDispatchToQueueNavigator(PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM)) {
                queueNavigator.onSkipToQueueItem(player, id);
            }
        }

        @Override
        public void onCustomAction(@NonNull String action, @Nullable Bundle extras) {
            Map<String, CustomActionProvider> actionMap = customActionMap;
            if (actionMap.containsKey(action)) {
                actionMap.get(action).onCustomAction(action, extras);
                updateMediaSessionPlaybackState();
            }
        }

        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            CommandReceiver commandReceiver = commandMap.get(command);
            if (commandReceiver != null) {
                commandReceiver.onCommand(player, command, extras, cb);
            }
        }

        @Override
        public void onPrepare() {
            if (canDispatchToPlaybackPreparer(PlaybackStateCompat.ACTION_PREPARE)) {
                player.stop();
                player.setPlayWhenReady(false);
                playbackPreparer.onPrepare();
            }
        }

        @Override
        public void onPrepareFromMediaId(String mediaId, Bundle extras) {
            if (canDispatchToPlaybackPreparer(PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID)) {
                player.stop();
                player.setPlayWhenReady(false);
                playbackPreparer.onPrepareFromMediaId(mediaId, extras);
            }
        }

        @Override
        public void onPrepareFromSearch(String query, Bundle extras) {
            if (canDispatchToPlaybackPreparer(PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH)) {
                player.stop();
                player.setPlayWhenReady(false);
                playbackPreparer.onPrepareFromSearch(query, extras);
            }
        }

        @Override
        public void onPrepareFromUri(Uri uri, Bundle extras) {
            if (canDispatchToPlaybackPreparer(PlaybackStateCompat.ACTION_PREPARE_FROM_URI)) {
                player.stop();
                player.setPlayWhenReady(false);
                playbackPreparer.onPrepareFromUri(uri, extras);
            }
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            if (canDispatchToPlaybackPreparer(PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID)) {
                player.stop();
                player.setPlayWhenReady(true);
                playbackPreparer.onPrepareFromMediaId(mediaId, extras);
            }
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            if (canDispatchToPlaybackPreparer(PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH)) {
                player.stop();
                player.setPlayWhenReady(true);
                playbackPreparer.onPrepareFromSearch(query, extras);
            }
        }

        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            if (canDispatchToPlaybackPreparer(PlaybackStateCompat.ACTION_PLAY_FROM_URI)) {
                player.stop();
                player.setPlayWhenReady(true);
                playbackPreparer.onPrepareFromUri(uri, extras);
            }
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description) {
            if (queueEditor != null) {
                queueEditor.onAddQueueItem(player, description);
            }
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description, int index) {
            if (queueEditor != null) {
                queueEditor.onAddQueueItem(player, description, index);
            }
        }

        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
            if (queueEditor != null) {
                queueEditor.onRemoveQueueItem(player, description);
            }
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onRemoveQueueItemAt(int index) {
            if (queueEditor != null) {
                queueEditor.onRemoveQueueItemAt(player, index);
            }
        }

        @Override
        public void onSetRating(RatingCompat rating) {
            if (canDispatchToQueueEditor(PlaybackStateCompat.ACTION_SET_RATING)) {
                queueEditor.onSetRating(player, rating);
            }
        }

    }

}

