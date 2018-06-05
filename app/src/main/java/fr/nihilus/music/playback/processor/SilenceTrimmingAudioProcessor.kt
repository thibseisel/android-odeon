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

import android.support.annotation.IntDef
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.audio.AudioProcessor
import java.nio.ByteBuffer

private const val STATE_NOISY = 0
private const val STATE_MAYBE_SILENT = 1
private const val STATE_SILENT = 2

private const val MINIMUM_SILENCE_DURATION_US = 100_000L
private const val PADDING_SILENCE_US = 10_000L
private const val SILENCE_THRESHOLD_LEVEL: Short = 1024
private const val SILENCE_THRESHOLD_LEVEL_MSB = ((SILENCE_THRESHOLD_LEVEL + 128) shr 8).toByte()

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
@IntDef(STATE_NOISY, STATE_MAYBE_SILENT, STATE_SILENT)
private annotation class State

/**
 * An [AudioProcessor] that trims silence from the start and the end of the input stream.
 * Input and output are 16-bits PCM.
 */
class SilenceTrimmingAudioProcessor : AudioProcessor {

    private var channelCount = Format.NO_VALUE
    private var sampleRateHz = Format.NO_VALUE
    private var bytesPerFrame = 0

    private var enabled: Boolean = false

    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    private var state: @State Int = STATE_NOISY
    private var maybeSilenceBufferSize = 0
    private var hasOutputNoise = false
    private var skippedFrames = 0L

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        flush()
    }

    fun getSkippedFrames(): Long = skippedFrames

    override fun configure(sampleRateHz: Int, channelCount: Int, encoding: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isActive(): Boolean = sampleRateHz != Format.NO_VALUE && enabled

    override fun getOutputChannelCount(): Int = channelCount

    @C.Encoding
    override fun getOutputEncoding(): Int = C.ENCODING_PCM_16BIT

    override fun queueEndOfStream() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOutputSampleRateHz(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun flush() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun queueInput(buffer: ByteBuffer?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isEnded(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOutput(): ByteBuffer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun reset() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}