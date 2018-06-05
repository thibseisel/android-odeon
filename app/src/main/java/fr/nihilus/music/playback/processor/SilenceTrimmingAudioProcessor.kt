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

import com.google.android.exoplayer2.audio.AudioProcessor
import java.nio.ByteBuffer

/**
 * An [AudioProcessor] that trims silence from the start and the end of the input stream.
 * Input and output are 16-bits PCM.
 */
class SilenceTrimmingAudioProcessor : AudioProcessor {

    fun setEnabled(enabled: Boolean) {
        TODO("Not implemented")
    }

    fun getSkippedFrames(): Long {
        TODO("Not implemented")
    }

    override fun isActive(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun queueEndOfStream() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun configure(sampleRateHz: Int, channelCount: Int, encoding: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOutputEncoding(): Int {
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

    override fun getOutputChannelCount(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}