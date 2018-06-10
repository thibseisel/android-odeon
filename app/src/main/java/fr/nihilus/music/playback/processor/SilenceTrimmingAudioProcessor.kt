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
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * The minimum duration of audio that must be below [SILENCE_THRESHOLD_LEVEL]
 * to classify that part of audio as silent, in microseconds.
 */
private const val MINIMUM_SILENCE_DURATION_US = 100_000L

private const val MINIMUM_END_SILENCE_DURATION_US = 1_000_000L
/**
 * The duration of silence by which to extend non-silent sections, in microseconds.
 * The value must not exceed [MINIMUM_SILENCE_DURATION_US].
 */
private const val PADDING_SILENCE_US = 10_000L
/**
 * The absolute level below which an individual PCM sample is classified as silent. Note: the
 * specified value will be rounded so that the threshold check only depends on the more
 * significant byte, for efficiency.
 */
private const val SILENCE_THRESHOLD_LEVEL: Short = 1024
/**
 * Threshold for classifying an individual PCM sample as silent based on its more significant byte.
 * This is [SILENCE_THRESHOLD_LEVEL] divided by 256 with rounding.
 */
private const val SILENCE_THRESHOLD_LEVEL_MSB = ((SILENCE_THRESHOLD_LEVEL + 128) shr 8).toByte()

/**
 * Enumeration of trimming states.
 */
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
@IntDef(STATE_NOISY, STATE_MAYBE_SILENT, STATE_SILENT)
private annotation class State

/** The input is not silent. */
private const val STATE_NOISY = 0
/** The input may be silent but we haven't read enough yet to know. */
private const val STATE_MAYBE_SILENT = 1
/** The input is silent. */
private const val STATE_SILENT = 2

/**
 * An [AudioProcessor] that trims silence from the start and the end of the input stream.
 * Input and output are 16-bits PCM.
 */
class SilenceTrimmingAudioProcessor : AudioProcessor {

    /** An empty byte array allocated only once for reuse. */
    private val EMPTY_BYTE_ARRAY = ByteArray(0)

    /** The output channel count. This is the same as the configured input. */
    private var channelCount = Format.NO_VALUE
    /** The output sample rate. This is the same as the configured input. */
    private var sampleRateHz = Format.NO_VALUE
    private var bytesPerFrame = 0

    private var enabled = false

    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    private var state: @State Int = STATE_NOISY
    private var maybeSilenceBufferSize = 0
    private var skippedFrames = 0L

    private var maybeSilenceBuffer: ByteArray = EMPTY_BYTE_ARRAY
    private var hasOutputNoise = false

    /**
     * Sets whether to trim silence in the input.
     * Calling this method will discard any data buffered within the processor,
     * and may update the value returned by [isActive].
     *
     * @param enabled Whether to trim silence in the input.
     */
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        flush()
    }

    /**
     * Returns the total number of frames of input audio that were skipped
     * due to being classified as silence since the last call to [flush].
     */
    fun getSkippedFrames(): Long = skippedFrames

    @Throws(AudioProcessor.UnhandledFormatException::class)
    override fun configure(sampleRateHz: Int, channelCount: Int, encoding: Int): Boolean {
        // This processor only supports PCM 16-bit input.
        if (encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledFormatException(sampleRateHz, channelCount, encoding)
        }

        // If sample rate and channel count haven't changed, no need to flush.
        if (this.sampleRateHz == sampleRateHz && this.channelCount == channelCount) {
            return false
        }

        // Otherwise, configure sample rate and channel count, then flush.
        this.sampleRateHz = sampleRateHz
        this.channelCount = channelCount
        bytesPerFrame = channelCount * 2
        return true
    }

    // The processor must be configured and enabled to be considered active.
    override fun isActive(): Boolean = sampleRateHz != Format.NO_VALUE && enabled

    // Output channel count is the same as the input.
    override fun getOutputChannelCount(): Int = channelCount

    // Output encoding should be 16-bits PCM.
    @C.Encoding override fun getOutputEncoding(): Int = C.ENCODING_PCM_16BIT

    // Output sample rate is the same as the input.
    override fun getOutputSampleRateHz(): Int = sampleRateHz

    override fun queueInput(inputBuffer: ByteBuffer) {
        while (inputBuffer.hasRemaining() && !outputBuffer.hasRemaining()) {
            when (state) {
                STATE_NOISY -> processNoisy(inputBuffer)
                STATE_MAYBE_SILENT -> processMaybeSilence(inputBuffer)
                STATE_SILENT -> processSilence(inputBuffer)
                else -> NoWhenBranchMatchedException("Unexpected state code: $state")
            }
        }
    }

    override fun queueEndOfStream() {
        inputEnded = true
        if (maybeSilenceBufferSize > 0) {
            // We haven't received enough silence to transition to the silent state, so output the buffer
            output(maybeSilenceBuffer, maybeSilenceBufferSize)
        }
    }

    override fun getOutput(): ByteBuffer {
        val outputBuffer = this.outputBuffer
        this.outputBuffer = AudioProcessor.EMPTY_BUFFER
        return outputBuffer
    }

    override fun isEnded() = inputEnded && outputBuffer === AudioProcessor.EMPTY_BUFFER

    override fun flush() {
        if (isActive) {
            // Calculate the required buffer size for silence and padding.
            val maybeSilenceBufferSize = durationToFrames(MINIMUM_SILENCE_DURATION_US) * bytesPerFrame
            if (maybeSilenceBuffer.size != maybeSilenceBufferSize) {
                maybeSilenceBuffer = ByteArray(maybeSilenceBufferSize)
            }
        }

        state = STATE_NOISY
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        skippedFrames = 0
        maybeSilenceBufferSize = 0
        hasOutputNoise = false
    }

    override fun reset() {
        enabled = false
        flush()
        buffer = AudioProcessor.EMPTY_BUFFER
        channelCount = Format.NO_VALUE
        sampleRateHz = Format.NO_VALUE
        maybeSilenceBuffer = EMPTY_BYTE_ARRAY
    }

    /**
     * Incrementally processes new input from [inputBuffer] while in [STATE_NOISY],
     * updating the state if needed.
     */
    private fun processNoisy(inputBuffer: ByteBuffer) {
        val limit = inputBuffer.limit()

        // Check if there's any noise within the maybe silence buffer duration.
        inputBuffer.limit(minOf(limit, inputBuffer.position() + maybeSilenceBuffer.size))
        val noiseLimit = findNoiseLimit(inputBuffer)
        if (noiseLimit == inputBuffer.position()) {
            // The buffer contains the start of possible silence.
            state = STATE_MAYBE_SILENT
        } else {
            inputBuffer.limit(noiseLimit)
            output(inputBuffer)
        }

        // Restore the limit.
        inputBuffer.limit(limit)
    }

    /**
     * Incrementally processes new input from [inputBuffer] while in [STATE_MAYBE_SILENT],
     * updating the state if needed.
     */
    private fun processMaybeSilence(inputBuffer: ByteBuffer) {
        val limit = inputBuffer.limit()
        val noisePosition = findNoisePosition(inputBuffer)
        val maybeSilenceInputSize = noisePosition - inputBuffer.position()
        val maybeSilenceBufferRemaining = maybeSilenceBuffer.size - maybeSilenceBufferSize
        if (noisePosition < limit && maybeSilenceInputSize < maybeSilenceBufferRemaining) {
            // The maybe silence buffer isn't full, so output it and switch back to the noisy state.
            output(maybeSilenceBuffer, maybeSilenceBufferSize)
            maybeSilenceBufferSize = 0
            state = STATE_NOISY
        } else {
            // Fill as much of the maybe silence buffer as possible.
            val bytesToWrite = minOf(maybeSilenceInputSize, maybeSilenceBufferRemaining)
            inputBuffer.limit(inputBuffer.position() + bytesToWrite)
            inputBuffer.get(maybeSilenceBuffer, maybeSilenceBufferSize, bytesToWrite)
            maybeSilenceBufferSize += bytesToWrite
            if (maybeSilenceBufferSize == maybeSilenceBuffer.size) {
                // We've reached a period of silence, so skip it.
                skippedFrames += (maybeSilenceBufferSize / bytesPerFrame)
                inputBuffer.position(inputBuffer.limit())
                maybeSilenceBufferSize = 0
                state = STATE_SILENT
            }

            // Restore the limit.
            inputBuffer.limit(limit)
        }
    }

    /**
     * Incrementally processes new input from [inputBuffer] while in [STATE_SILENT],
     * updating the state if needed.
     */
    private fun processSilence(inputBuffer: ByteBuffer) {
        val limit = inputBuffer.limit()
        val noisyPosition = findNoisePosition(inputBuffer)
        inputBuffer.limit(noisyPosition)
        skippedFrames += inputBuffer.remaining() / bytesPerFrame
        inputBuffer.position(noisyPosition)
        if (noisyPosition < limit) {
            // Transition back to the noisy state.
            state = STATE_NOISY
            // Restore the limit.
            inputBuffer.limit(limit)
        }
    }

    /**
     * Copies [length] elements from [data] to populate a new output buffer from the processor.
     */
    private fun output(data: ByteArray, length: Int) {
        prepareForOutput(length)
        buffer.put(data, 0, length)
        buffer.flip()
        outputBuffer = buffer
    }

    /**
     * Copies remaining bytes from [data] to populate a new output buffer from the processor.
     */
    private fun output(data: ByteBuffer) {
        prepareForOutput(data.remaining())
        buffer.put(data)
        buffer.flip()
        outputBuffer = buffer
    }

    /**
     * Prepares to output [size] bytes in [buffer].
     */
    private fun prepareForOutput(size: Int) {
        if (buffer.capacity() < size) {
            buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
        } else {
            buffer.clear()
        }

        if (size > 0) {
            if (!hasOutputNoise) {
                val maybeSilenceBufferSize = durationToFrames(MINIMUM_END_SILENCE_DURATION_US) * bytesPerFrame
                maybeSilenceBuffer = ByteArray(maybeSilenceBufferSize).also {
                    System.arraycopy(maybeSilenceBuffer, 0, it, 0, maybeSilenceBuffer.size)
                }
            }

            hasOutputNoise = true
        }
    }

    /**
     * Returns the number of input frames corresponding to [durationUs] microseconds of audio.
     */
    private fun durationToFrames(durationUs: Long): Int =
        ((durationUs * sampleRateHz) / C.MICROS_PER_SECOND).toInt()

    /**
     * Returns the earliest byte position in `[position, limit)` of [buffer]
     * that contains a frame classified as a noisy frame,
     * or the limit of the buffer if no such frame exists.
     */
    private fun findNoisePosition(buffer: ByteBuffer): Int {
        // The input is in native order, which is always little endian on Android.
        // We only read the second byte for each frame, which is the most significant one.
        var i = buffer.position() + 1
        while (i < buffer.limit()) {
            // If the sound level of that frame exceeds the threshold, return its frame position
            if (abs(buffer[i].toInt()) > SILENCE_THRESHOLD_LEVEL_MSB) {
                // Round to the start of the frame
                return bytesPerFrame * (i / bytesPerFrame)
            }
            i += 2
        }

        return buffer.limit()
    }

    /**
     * Returns the earliest byte position in `[position, limit)` of [buffer]
     * such that all frames from the byte position to the limit are classified as silent.
     */
    private fun findNoiseLimit(buffer: ByteBuffer): Int {
        var i = buffer.limit() - 1
        while (i >= buffer.position()) {
            if (abs(buffer[i].toInt()) > SILENCE_THRESHOLD_LEVEL_MSB) {
                // Return the start of the next frame.
                return bytesPerFrame * (i / bytesPerFrame) + bytesPerFrame
            }
            i -= 2
        }

        return buffer.position()
    }
}