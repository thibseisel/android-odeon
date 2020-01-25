/*
 * Copyright 2020 Thibault Seisel
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

package fr.nihilus.music.glide

import android.content.ContentResolver
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.data.LocalUriFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

/**
 * Decodes [Audio MediaStore][MediaStore.Audio] [Uri]s into [InputStream]s.
 *
 * As of Android Q, album artworks could not longer be retrieved as a file path in [MediaStore.Audio.Albums.ALBUM_ART] ;
 * otherwise artworks are accessed from the content uri of its track or its album.
 *
 * @constructor
 * @param resolver The [ContentResolver] used to decode MediaStore uris.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class MediaStoreAlbumArtworkLoader private constructor(
    private val resolver: ContentResolver
) : ModelLoader<Uri, InputStream> {

    override fun handles(model: Uri): Boolean = isMediaStoreAudioUri(model)

    private fun isMediaStoreAudioUri(uri: Uri): Boolean =
        ContentResolver.SCHEME_CONTENT == uri.scheme
                && MediaStore.AUTHORITY == uri.authority
                && "audio" in uri.pathSegments

    override fun buildLoadData(
        model: Uri,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream>? {
        val diskCacheKey = ObjectKey(model)
        val fetcher = InputStreamFetcher(resolver, model, width, height)
        return ModelLoader.LoadData(diskCacheKey, fetcher)
    }

    /**
     * Factory for creating [MediaStoreAlbumArtworkLoader].
     *
     * @param resolver The [ContentResolver] used to decode [MediaStore] uris.
     */
    class InputStreamFactory(
        private val resolver: ContentResolver
    ) : ModelLoaderFactory<Uri, InputStream> {

        override fun build(
            multiFactory: MultiModelLoaderFactory
        ): ModelLoader<Uri, InputStream> = MediaStoreAlbumArtworkLoader(resolver)

        override fun teardown() {
            // Do nothing.
        }
    }

    /**
     * A [DataFetcher] that resolves a local [Uri] to load an album artwork for a [MediaStore] audio resource.
     * This internally uses [ContentResolver.openTypedAssetFile] to open a stream to the artwork,
     * specifying an optimal size in case multiple scaled versions of the artwork are available.
     *
     * While [ContentResolver.loadThumbnail] is the official way of loading those artworks,
     * we preferred [ContentResolver.openTypedAssetFile] (which is called internally by `loadThumbnail`)
     * as it produces an [InputStream], allowing Glide to decode it more efficiently.
     *
     * @param resolver ContentResolver used to decode [MediaStore] [Uri]s.
     * @param uri The uri pointing to a local track or album resource.
     * @param width The desired width of the loaded artwork.
     * @param height The desired height of the loaded artwork.
     */
    private class InputStreamFetcher(
        resolver: ContentResolver,
        uri: Uri,
        private val width: Int,
        private val height: Int
    ) : LocalUriFetcher<InputStream>(resolver, uri) {

        override fun getDataClass(): Class<InputStream> = InputStream::class.java

        @Throws(FileNotFoundException::class)
        override fun loadResource(uri: Uri, contentResolver: ContentResolver): InputStream {
            val optimalSizeOptions = Bundle(1)
            optimalSizeOptions.putParcelable(ContentResolver.EXTRA_SIZE, Point(width, height))

            return contentResolver.openTypedAssetFile(uri, "image/*", optimalSizeOptions, null)
                ?.createInputStream()
                ?: throw FileNotFoundException("FileDescriptor is null for: $uri")
        }

        @Throws(IOException::class)
        override fun close(data: InputStream) {
            data.close()
        }
    }
}