/*
 * Copyright 2018 Thibault Seisel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.nihilus.music.playback.processor

import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.audio.AudioProcessor
import com.google.android.exoplayer2.audio.DefaultAudioSink
import com.google.android.exoplayer2.audio.SonicAudioProcessor

/**
 * Defines a custom chain of processors to be used for audio-decoding.
 *
 * This overrides the default [DefaultAudioSink.DefaultAudioProcessorChain]
 * to replace the "silence skipping" feature with a softer "silence trimming"
 * that only removes silences from the start and the end of the track,
 * therefore not altering the quality of the original track.
 *
 * That feature is enabled via [PlaybackParameters].
 */
class CustomAudioProcessorChain(
    vararg audioProcessors: AudioProcessor
) : DefaultAudioSink.AudioProcessorChain {

    private val silenceProcessor = SilenceTrimmingAudioProcessor()
    private val sonicProcessor = SonicAudioProcessor()
    private val audioProcessors = arrayOf(*audioProcessors, silenceProcessor, sonicProcessor)

    override fun applyPlaybackParameters(playbackParameters: PlaybackParameters): PlaybackParameters {
        silenceProcessor.setEnabled(playbackParameters.skipSilence)
        return PlaybackParameters(
            sonicProcessor.setPitch(playbackParameters.pitch),
            sonicProcessor.setSpeed(playbackParameters.speed),
            playbackParameters.skipSilence
        )
    }

    override fun getMediaDuration(playoutDuration: Long): Long =
        sonicProcessor.scaleDurationForSpeedup(playoutDuration)

    override fun getSkippedOutputFrameCount(): Long = silenceProcessor.getSkippedFrames()

    override fun getAudioProcessors(): Array<AudioProcessor> = audioProcessors
}