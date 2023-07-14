/*
 * Copyright 2021 Thibault Seisel
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

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.flac.FlacExtractor
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.extractor.ogg.OggExtractor
import androidx.media3.extractor.ts.Ac3Extractor
import androidx.media3.extractor.ts.AdtsExtractor
import androidx.media3.extractor.wav.WavExtractor

/**
 * An ExtractorsFactory that only uses audio file extractors.
 * Unlike [DefaultExtractorsFactory] that internally references all [Extractor] implementations,
 * this factory does not reference extractors it doesn't need,
 * thus allowing Proguard from removing unused extractors and reducing final APK size.
 *
 * The full explanation is detailed
 * on the [ExoPlayer official documentation](https://google.github.io/ExoPlayer/shrinking.html).
 */
@OptIn(UnstableApi::class)
internal class AudioOnlyExtractorsFactory : ExtractorsFactory {

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
        // Apple .m4a (which is in fact the same format as MPEG-4)
        Mp4Extractor(),
        // .flac audio files for API 27+
        FlacExtractor()
    )
}
