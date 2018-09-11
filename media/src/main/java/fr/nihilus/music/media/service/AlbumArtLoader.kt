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

package fr.nihilus.music.media.service

import android.support.v4.media.MediaMetadataCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions
import fr.nihilus.music.media.extensions.copy
import fr.nihilus.music.media.di.ServiceScoped
import fr.nihilus.music.media.extensions.albumArt
import fr.nihilus.music.media.extensions.albumArtUri
import io.reactivex.Single
import javax.inject.Inject

private const val ART_MAX_SIZE = 320

@ServiceScoped
internal class AlbumArtLoader
@Inject constructor(context: MusicService) {

    private val glide = Glide.with(context).asBitmap()
        .apply(RequestOptions().downsample(DownsampleStrategy.AT_MOST))

    fun loadIntoMetadata(metadata: MediaMetadataCompat): Single<MediaMetadataCompat> {
        return Single.create { emitter ->
            val artUri = metadata.albumArtUri
            if (artUri != null) {
                val loadedAlbumArt = glide.load(artUri).submit(ART_MAX_SIZE, ART_MAX_SIZE).get()
                emitter.onSuccess(metadata.copy {
                    albumArt = loadedAlbumArt
                })
            } else {
                // When there's no album art, ee-emit the same metadata without modifications.
                emitter.onSuccess(metadata)
            }
        }
    }
}