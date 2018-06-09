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

import com.google.android.exoplayer2.C
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

private const val SIGNAL_SAMPLE_RATE_HZ = 1000
private const val SIGNAL_CHANNEL_COUNT = 2
private const val SIGNAL_SILENCE_DURATION_MS = 1000
private const val SIGNAL_NOISE_DURATION_MS = 1000
private const val SIGNAL_FRAME_COUNT = 100000

private const val INPUT_BUFFER_SIZE = 100

class SilenceTrimmingAudioProcessorTest {

    private lateinit var processor: SilenceTrimmingAudioProcessor

    @Before
    fun setUp() {
        processor = SilenceTrimmingAudioProcessor()
    }

    @Test
    fun testEnabledProcessor_isActive() {
        // Given an enabled processor
        processor.setEnabled(true)

        // When configuring it...
        val reconfigured = processor.configure(
            SIGNAL_SAMPLE_RATE_HZ, SIGNAL_CHANNEL_COUNT, C.ENCODING_PCM_16BIT)
        processor.flush()

        // It's active.
        assertThat(reconfigured).isTrue()
        assertThat(processor.isActive).isTrue()
    }

    @Test
    fun testDisabledProcessor_isNotActive() {
        // Given a disabled processor
        processor.setEnabled(false)

        // When reconfiguring it ...
        processor.configure(SIGNAL_SAMPLE_RATE_HZ, SIGNAL_CHANNEL_COUNT, C.ENCODING_PCM_16BIT)

        // It's not active
        assertThat(processor.isActive).isFalse()
    }

    @Test
    fun testChangingSampleRate_requiresReconfiguration() {
        // Given an enabled and configured processor
        processor.setEnabled(true)
        var reconfigured = processor.configure(
            SIGNAL_SAMPLE_RATE_HZ, SIGNAL_CHANNEL_COUNT, C.ENCODING_PCM_16BIT)
        if (reconfigured) {
            processor.flush()
        }

        // When reconfiguring it with a different sample rate...
        reconfigured = processor.configure(
            SIGNAL_SAMPLE_RATE_HZ * 2, SIGNAL_CHANNEL_COUNT, C.ENCODING_PCM_16BIT)

        // It's reconfigured.
        assertThat(reconfigured).isTrue()
        assertThat(processor.isActive).isTrue()
    }

    @Test
    fun testReconfiguringWithSameSampleRate_doesNotRequireReconfiguration() {
        // Given an enabled and configured processor
        processor.setEnabled(true)
        var reconfigured = processor.configure(SIGNAL_SAMPLE_RATE_HZ, SIGNAL_CHANNEL_COUNT, C.ENCODING_PCM_16BIT)
        assertThat(reconfigured).isTrue()
        processor.flush()

        // When reconfiguring it with the same sample rate...
        reconfigured = processor.configure(SIGNAL_SAMPLE_RATE_HZ, SIGNAL_CHANNEL_COUNT, C.ENCODING_PCM_16BIT)

        // Its not reconfigured but it's active.
        assertThat(reconfigured).isFalse()
        assertThat(processor.isActive).isTrue()
    }

    @Test
    fun testTrimInSilentSignal_skipsEverything() {
        // Given a signal with only silence
        val inputBufferProvider = getInputBufferProviderForAlternatingSilenceAndNoise(
            SIGNAL_SAMPLE_RATE_HZ,
            SIGNAL_CHANNEL_COUNT,
            SIGNAL_SILENCE_DURATION_MS,
            0,
            SIGNAL_FRAME_COUNT
        )

        // When processing the entire signal...
        enableAndConfigureProcessor()
        val totalOutputFrames = process(processor, inputBufferProvider, INPUT_BUFFER_SIZE)

        // The entire signal is skipped.
        assertThat(totalOutputFrames).isEqualTo(0L)
        assertThat(processor.getSkippedFrames()).isEqualTo(SIGNAL_FRAME_COUNT)
    }

    @Test
    fun testTrimNoisySignal_skipsNothing() {
        // Given a signal with only noise
        val inputBufferProvider = getInputBufferProviderForAlternatingSilenceAndNoise(
            SIGNAL_SAMPLE_RATE_HZ,
            SIGNAL_CHANNEL_COUNT,
            0,
            SIGNAL_NOISE_DURATION_MS,
            SIGNAL_FRAME_COUNT
        )

        // When processing the entire signal...
        enableAndConfigureProcessor()
        val totalOutputFrames = process(processor, inputBufferProvider, INPUT_BUFFER_SIZE)

        // None of the signal is skipped.
        assertThat(totalOutputFrames).isEqualTo(SIGNAL_FRAME_COUNT)
        assertThat(processor.getSkippedFrames()).isEqualTo(0L)
    }

    @Test
    fun testTrimStartSilence_skipsStartOfSignal() {
        // Given a signal starting with 1 second of silence ...
        val silenceFrameCount = 1 * SIGNAL_SAMPLE_RATE_HZ // 1 second of silence
        val noiseFrameCount = SIGNAL_FRAME_COUNT - 1 * SIGNAL_SAMPLE_RATE_HZ // The rest as noise
        val inputBufferProvider = buildInputBufferProvider(SIGNAL_CHANNEL_COUNT, SIGNAL_FRAME_COUNT) {
            appendFrames(silenceFrameCount, 0, 0)
            appendFrames(noiseFrameCount, Short.MAX_VALUE, Short.MAX_VALUE)
        }

        enableAndConfigureProcessor()
        val totalOutputFrames = process(processor, inputBufferProvider, INPUT_BUFFER_SIZE)

        assertThat(totalOutputFrames).isEqualTo(noiseFrameCount)
        assertThat(processor.getSkippedFrames()).isEqualTo(silenceFrameCount)
    }

    @Test
    fun testTrimEndSilence_skipsEndOfSignal() {
        // Given a signal ending with 1 second of silence ...
        val silenceFrameCount = 1 * SIGNAL_SAMPLE_RATE_HZ // 1 second of silence
        val noiseFrameCount = SIGNAL_FRAME_COUNT - 1 * SIGNAL_SAMPLE_RATE_HZ // The rest as noise
        val inputBufferProvider = buildInputBufferProvider(SIGNAL_CHANNEL_COUNT, SIGNAL_FRAME_COUNT) {
            appendFrames(noiseFrameCount, Short.MAX_VALUE, Short.MAX_VALUE)
            appendFrames(silenceFrameCount, 0, 0)
        }

        enableAndConfigureProcessor()
        val totalOutputFrames = process(processor, inputBufferProvider, INPUT_BUFFER_SIZE)

        assertThat(totalOutputFrames).isEqualTo(noiseFrameCount)
        assertThat(processor.getSkippedFrames()).isEqualTo(silenceFrameCount)
    }

    @Test
    fun testMiddleSilence_skipsNothing() {
        // Given a signal with 1 second of silence in the middle of noise ...
        val silenceFrameCount = 1 * SIGNAL_SAMPLE_RATE_HZ // 1 second of silence
        val noiseFrameCount = SIGNAL_FRAME_COUNT - 1 * SIGNAL_SAMPLE_RATE_HZ // The rest as noise
        val inputBufferProvider = buildInputBufferProvider(SIGNAL_CHANNEL_COUNT, SIGNAL_FRAME_COUNT) {
            appendFrames(noiseFrameCount / 2, Short.MAX_VALUE, Short.MAX_VALUE)
            appendFrames(silenceFrameCount, 0, 0)
            appendFrames(noiseFrameCount / 2, Short.MAX_VALUE, Short.MAX_VALUE)
        }

        enableAndConfigureProcessor()
        val totalOutputFrames = process(processor, inputBufferProvider, INPUT_BUFFER_SIZE)

        assertThat(totalOutputFrames).isEqualTo(SIGNAL_FRAME_COUNT)
        assertThat(processor.getSkippedFrames()).isEqualTo(0)
    }

    @Test
    fun testStartEndSilence_skipsStartAndEndSilence() {
        // Given a signal between 2 silences of 1 second
        val silenceFrameCount = 1 * SIGNAL_SAMPLE_RATE_HZ
        val noiseFrameCount = SIGNAL_FRAME_COUNT - 2 * SIGNAL_SAMPLE_RATE_HZ
        val inputBufferProvider = buildInputBufferProvider(SIGNAL_CHANNEL_COUNT, SIGNAL_FRAME_COUNT) {
            appendFrames(silenceFrameCount, 0, 0)
            appendFrames(noiseFrameCount, Short.MAX_VALUE, Short.MAX_VALUE)
            appendFrames(silenceFrameCount, 0, 0)
        }

        enableAndConfigureProcessor()
        val totalOutputFrames = process(processor, inputBufferProvider, INPUT_BUFFER_SIZE)

        assertThat(totalOutputFrames).isEqualTo(noiseFrameCount)
        assertThat(processor.getSkippedFrames()).isEqualTo(2 * silenceFrameCount)
    }

    @Test
    fun testStartMiddleEndSilence_skipsStartAndEndSilence() {
        val silenceFrameCount = 1 * SIGNAL_SAMPLE_RATE_HZ
        val noiseFrameCount = SIGNAL_FRAME_COUNT - 3 * SIGNAL_SAMPLE_RATE_HZ
        val inputBufferProvider = buildInputBufferProvider(SIGNAL_CHANNEL_COUNT, SIGNAL_FRAME_COUNT) {
            appendFrames(silenceFrameCount, 0, 0)
            appendFrames(noiseFrameCount / 2, Short.MAX_VALUE, Short.MAX_VALUE)
            appendFrames(silenceFrameCount, 0, 0)
            appendFrames(noiseFrameCount / 2, Short.MAX_VALUE, Short.MAX_VALUE)
            appendFrames(silenceFrameCount, 0, 0)
        }

        enableAndConfigureProcessor()
        val totalOutputFrames = process(processor, inputBufferProvider, INPUT_BUFFER_SIZE)

        assertThat(totalOutputFrames).isEqualTo(noiseFrameCount + silenceFrameCount)
        assertThat(processor.getSkippedFrames()).isEqualTo(2 * noiseFrameCount)
    }

    @After
    fun tearDown() {
        processor.reset()
    }

    private fun enableAndConfigureProcessor() {
        processor.setEnabled(true)
        val reconfigured = processor.configure(SIGNAL_SAMPLE_RATE_HZ, SIGNAL_CHANNEL_COUNT, C.ENCODING_PCM_16BIT)
        processor.flush()
        assertThat(reconfigured).isTrue()
        assertThat(processor.isActive).isTrue()
    }
}

/**
 * Processes the entire stream provided by [inputBufferProvider] in chunks of [inputBufferSize]
 * and returns the total number of output frames.
 */
private fun process(
    processor: SilenceTrimmingAudioProcessor,
    inputBufferProvider: InputBufferProvider,
    inputBufferSize: Int
): Long {
    processor.flush()
    var totalOutputFrames = 0L
    while (inputBufferProvider.hasRemaining()) {
        val inputBuffer = inputBufferProvider.getNextInputBuffer(inputBufferSize)
        while (inputBuffer.hasRemaining()) {
            processor.queueInput(inputBuffer)
            val outputBuffer = processor.output
            totalOutputFrames += outputBuffer.remaining() / (2 * processor.outputChannelCount)
            outputBuffer.clear()
        }
    }

    processor.queueEndOfStream()
    while (!processor.isEnded) {
        val outputBuffer = processor.output
        totalOutputFrames += outputBuffer.remaining() / (2 * processor.outputChannelCount)
        outputBuffer.clear()
    }

    return totalOutputFrames
}

/**
 * Wraps a [ShortBuffer] and provides a sequence of [ByteBuffer]s of specified sizes
 * that contain copies of its data.
 */
private class InputBufferProvider(private val buffer: ShortBuffer) {

    /**
     * Returns the next buffer with size up to [sizeBytes].
     */
    fun getNextInputBuffer(sizeBytes: Int): ByteBuffer {
        val inputBuffer = ByteBuffer.allocate(sizeBytes).order(ByteOrder.nativeOrder())
        val inputBufferAsShorts = inputBuffer.asShortBuffer()
        val limit = buffer.limit()
        buffer.limit(minOf(buffer.position() + sizeBytes / 2, limit))
        inputBufferAsShorts.put(buffer)
        buffer.limit(limit)
        inputBuffer.limit(inputBufferAsShorts.position() * 2)
        return inputBuffer
    }

    /**
     * Returns whether any more input can be provided via [getNextInputBuffer].
     */
    fun hasRemaining(): Boolean = buffer.hasRemaining()
}

private inline fun buildInputBufferProvider(
    channelCount: Int,
    totalFrameCount: Int,
    bufferBuilder: Pcm16BitAudioBuilder.() -> Unit
): InputBufferProvider {
    val buffer = Pcm16BitAudioBuilder(channelCount, totalFrameCount).apply(bufferBuilder).build()
    return InputBufferProvider(buffer)
}

private fun getInputBufferWithSilences(
    sampleRateHz: Int,
    channelCount: Int,
    totalFrameCount: Int,
    noiseDurationMs: Int,
    startSilenceDurationMs: Int,
    middleSilenceDurationMs: Int,
    endSilenceDurationMs: Int
): InputBufferProvider {

    val totalFrameDurationMs = totalFrameCount * 1000 / sampleRateHz
    require(noiseDurationMs + startSilenceDurationMs + middleSilenceDurationMs + endSilenceDurationMs == totalFrameDurationMs) {
        "The total duration of noise and silences must be of $totalFrameDurationMs milliseconds."
    }

    val noiseFramesCount = (noiseDurationMs * sampleRateHz) / 1000
    val startSilenceFramesCount = (startSilenceDurationMs * sampleRateHz) / 1000
    val middleSilenceFramesCount = (middleSilenceDurationMs * sampleRateHz) / 1000
    val endSilenceFramesCount = (endSilenceDurationMs * sampleRateHz) / 1000

    val audioBuilder = Pcm16BitAudioBuilder(channelCount, totalFrameCount).apply {
        appendFrames(startSilenceFramesCount, 0, 0)
        appendFrames(noiseFramesCount / 2, Short.MAX_VALUE, Short.MAX_VALUE)
        appendFrames(middleSilenceFramesCount, 0, 0)
        appendFrames(noiseFramesCount / 2, Short.MAX_VALUE, Short.MAX_VALUE)
        appendFrames(endSilenceFramesCount, 0, 0)
    }

    return InputBufferProvider(audioBuilder.build())
}

/**
 * Returns an [InputBufferProvider] that provides input buffers for a stream
 * that alternates between silence/noise of the specified durations to fill [totalFrameCount].
 *
 * @param sampleRateHz The sample rate of the resulting signal in hertz.
 * @param channelCount The number of channels for the resulting signal.
 * @param silenceDurationMs The duration of silence periods in milliseconds.
 * @param noiseDurationMs The duration of noise periods in milliseconds.
 * @param totalFrameCount The total number of signal frames to be generated.
 */
private fun getInputBufferProviderForAlternatingSilenceAndNoise(
    sampleRateHz: Int,
    channelCount: Int,
    silenceDurationMs: Int,
    noiseDurationMs: Int,
    totalFrameCount: Int
): InputBufferProvider {
    val audioBuilder = Pcm16BitAudioBuilder(channelCount, totalFrameCount)
    while (!audioBuilder.isFull()) {
        val silenceDurationFrames = (silenceDurationMs * sampleRateHz) / 1000
        audioBuilder.appendFrames(silenceDurationFrames, 0, 0)
        val noiseDurationFrames = (noiseDurationMs * sampleRateHz) / 1000
        audioBuilder.appendFrames(noiseDurationFrames, Short.MAX_VALUE, Short.MAX_VALUE)
    }

    return InputBufferProvider(audioBuilder.build())
}

/**
 * Computes the number of audio frames for a given duration.
 *
 * @param durationMs The duration in milliseconds.
 * @param sampleRateHz The sample rate in Hertz.
 */
private fun durationToFrames(durationMs: Int, sampleRateHz: Int) = durationMs * sampleRateHz / 1000

/**
 * Builder for [ShortBuffer]s that contains 16-bit PCM audio samples.
 */
private class Pcm16BitAudioBuilder(
    private val channelCount: Int,
    frameCount: Int
) {
    private val buffer = ByteBuffer.allocate(frameCount * channelCount * 2).asShortBuffer()
    private var built: Boolean = false

    /**
     * Append [count] audio frames, using the specified [channelLevels] in each frames.
     *
     * @param count The number of frames to append to this audio sample sequence.
     * @param channelLevels The values for channels of each sample.
     * There should be at least [channelCount] values.
     */
    fun appendFrames(count: Int, vararg channelLevels: Short) {
        require(channelLevels.size == channelCount)
        check(!built)

        repeat(count) {
            for (level in channelLevels) {
                buffer.put(level)
            }
        }
    }

    /**
     * Returns whether the buffer is full.
     */
    fun isFull(): Boolean {
        check(!built)
        return !buffer.hasRemaining()
    }

    /**
     * Returns the built buffer.
     * After calling this method the builder should not be reused.
     */
    fun build(): ShortBuffer {
        check(!built)
        built = true
        buffer.flip()
        return buffer
    }
}