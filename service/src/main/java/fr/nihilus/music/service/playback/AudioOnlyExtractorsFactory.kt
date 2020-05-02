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

package fr.nihilus.music.service.playback

import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.Extractor
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.extractor.flac.FlacExtractor
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor
import com.google.android.exoplayer2.extractor.ogg.OggExtractor
import com.google.android.exoplayer2.extractor.ts.Ac3Extractor
import com.google.android.exoplayer2.extractor.ts.AdtsExtractor
import com.google.android.exoplayer2.extractor.wav.WavExtractor

/**
 * An ExtractorsFactory that only uses audio file extractors.
 * Unlike [DefaultExtractorsFactory] that internally references all [Extractor] implementations,
 * this factory does not reference extractors it doesn't need,
 * thus allowing Proguard from removing unused extractors and reducing final APK size.
 *
 * The full explanation is detailed
 * on the [ExoPlayer official documentation](https://google.github.io/ExoPlayer/shrinking.html).
 */
internal class AudioOnlyExtractorsFactory : ExtractorsFactory {

    @Synchronized
    override fun createExtractors() = arrayOf(
        // Most used audio file extensions .mp3 and .wav
        Mp3Extractor(),
        WavExtractor(),
        // .aac audio files
        AdtsExtractor(),
        // .ogg and .oga audio files
        OggExtractor(),
        // .ac3 (Dolby Digital) audio files
        Ac3Extractor(),
        // .flac audio files for API 27+
        FlacExtractor()
    )
}