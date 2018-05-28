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

package fr.nihilus.music.glide.palette

import android.graphics.Bitmap
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.ResourceEncoder
import com.bumptech.glide.load.data.BufferedOutputStream
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import fr.nihilus.music.ui.albums.AlbumPalette
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

private const val HEADER_AND_PALETTE_BYTE_SIZE = 3 + AlbumArtResource.ALBUM_PALETTE_BYTE_SIZE
private const val HEADER_A: Byte = 0x41
private const val HEADER_R: Byte = 0x52
private const val HEADER_T: Byte = 0x54

class AlbumArtEncoder(
    private val encoder: ResourceEncoder<Bitmap>,
    private val bitmapPool: BitmapPool,
    private val arrayPool: ArrayPool
) : ResourceEncoder<AlbumArt> {

    override fun getEncodeStrategy(options: Options) = encoder.getEncodeStrategy(options)

    override fun encode(data: Resource<AlbumArt>, file: File, options: Options): Boolean {
        Timber.d("Encoding AlbumArt to file ${file.name}")
        val (bitmap, palette) = data.get()
        val paletteBytes = arrayPool.get(HEADER_AND_PALETTE_BYTE_SIZE, ByteArray::class.java)

        // Write header as ASCII letters "ART"
        paletteBytes[0] = HEADER_A
        paletteBytes[1] = HEADER_R
        paletteBytes[2] = HEADER_T

        // Write each color as a 4-bytes integer
        paletteBytes.setInt(3, palette.primary)
        paletteBytes.setInt(7, palette.accent)
        paletteBytes.setInt(11, palette.titleText)
        paletteBytes.setInt(15, palette.bodyText)
        paletteBytes.setInt(19, palette.textOnAccent)

        try {
            BufferedOutputStream(file.outputStream(), arrayPool).use {
                it.write(paletteBytes, 0, HEADER_AND_PALETTE_BYTE_SIZE)
            }
        } catch (e: IOException) {
            Timber.e(e, "Error while writing AlbumPalette to cache file.")
        }

        return encoder.encode(BitmapResource(bitmap, bitmapPool), file, options)
    }
}

private fun ByteArray.setInt(startIndex: Int, value: Int) {
    assert(size > startIndex + 3)
    this[startIndex] = (value ushr 24).toByte()
    this[startIndex + 1] = (value ushr 16).toByte()
    this[startIndex + 2] = (value ushr 8).toByte()
    this[startIndex + 3] = value.toByte()
}

/**
 *
 */
class AlbumArtDecoder(
    private val decoder: ResourceDecoder<ByteBuffer, Bitmap>,
    private val arrayPool: ArrayPool
) : ResourceDecoder<ByteBuffer, AlbumArt> {

    @Throws(IOException::class)
    override fun handles(source: ByteBuffer, options: Options): Boolean {
        // Check that it remains enough bytes for the header and the palette
        if (source.remaining() < HEADER_AND_PALETTE_BYTE_SIZE) return false

        // Read the first 3 bytes to check if it matches the ASCII sequence "ART"
        val headerBytes = arrayPool.get(3, ByteArray::class.java)
        source.get(headerBytes, 0, 3)

        return headerBytes[0] == HEADER_A &&
                headerBytes[1] == HEADER_R &&
                headerBytes[2] == HEADER_T &&
                decoder.handles(source, options)
    }

    override fun decode(
        source: ByteBuffer,
        width: Int,
        height: Int,
        options: Options
    ): Resource<AlbumArt>? {
        Timber.d("Retrieving AlbumArt from $source")

        // Read each color as 4-bytes integers
        @Suppress("UsePropertyAccessSyntax")
        val palette = AlbumPalette(
            source.getInt(),
            source.getInt(),
            source.getInt(),
            source.getInt(),
            source.getInt()
        )

        val bitmapResource = decoder.decode(source, width, height, options)
        return if (bitmapResource != null) AlbumArtResource(palette, bitmapResource) else null
    }

}