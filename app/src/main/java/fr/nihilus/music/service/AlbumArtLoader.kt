/*
 * Copyright 2017 Thibault Seisel
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

package fr.nihilus.music.service

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.support.v4.media.MediaMetadataCompat
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import fr.nihilus.music.copy
import fr.nihilus.music.di.ServiceScoped
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.toUri
import io.reactivex.Single
import javax.inject.Inject

private const val TAG = "AlbumArtLoader"
private const val ART_MAX_SIZE = 320

@ServiceScoped
class AlbumArtLoader
@Inject internal constructor(context: MusicService) {

    private val glide = GlideApp.with(context).asBitmap()
        .downsample(DownsampleStrategy.AT_MOST)

    fun loadIntoMetadata(metadata: MediaMetadataCompat): Single<MediaMetadataCompat> {
        return Single.create { emitter ->
            val uriString = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
            if (uriString != null) {
                val artUri = uriString.toUri()
                glide.load(artUri).into(object : SimpleTarget<Bitmap>(ART_MAX_SIZE, ART_MAX_SIZE) {

                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        // Emits a new metadata with an album art
                        emitter.onSuccess(metadata.copy {
                            putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, resource)
                        })
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        // Re-emit the same metadata without modifications in case of an error.
                        emitter.onSuccess(metadata)
                    }
                })
            } else {
                // When there's no album art, ee-emit the same metadata without modifications.
                emitter.onSuccess(metadata)
            }
        }
    }
}