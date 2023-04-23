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

import android.content.Context
import android.os.Handler
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.text.TextOutput
import com.google.android.exoplayer2.video.VideoRendererEventListener

/**
 * A [RenderersFactory] implementation that only uses the audio renderer.
 * Unlike [DefaultRenderersFactory] that internally references all [Renderer] implementations,
 * this factory does not reference video renderers it doesn't need,
 * thus allowing Proguard from removing unused renderers and reducing final APK size.
 *
 * The full explanation is detailed on the
 * [ExoPlayer official documentation](https://google.github.io/ExoPlayer/shrinking.html).
 */
internal class AudioOnlyRenderersFactory(private val context: Context) : RenderersFactory {

    override fun createRenderers(
        eventHandler: Handler,
        videoRendererEventListener: VideoRendererEventListener,
        audioListener: AudioRendererEventListener,
        textRendererOutput: TextOutput,
        metadataRendererOutput: MetadataOutput
    ) = arrayOf<Renderer>(
        MediaCodecAudioRenderer(context, MediaCodecSelector.DEFAULT, eventHandler, audioListener)
    )
}
