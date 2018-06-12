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
import android.support.annotation.ColorInt
import android.support.v7.graphics.Palette
import android.support.v7.graphics.Target
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.ResourceEncoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import fr.nihilus.music.glide.GlideExtensions
import fr.nihilus.music.ui.albums.AlbumPalette
import fr.nihilus.music.utils.toHsl
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * The size of the header indicating that a cached [AlbumPalette] follows in the data stream.
 * This header is composed of the ASCII letters "ART" expressed as bytes.
 */
private const val HEADER_BYTE_SIZE = 3
private const val HEADER_A: Byte = 0x41
private const val HEADER_R: Byte = 0x52
private const val HEADER_T: Byte = 0x54

/**
 * Each AlbumPalette is composed of 5 four-bytes integers.
 */
private const val PALETTE_BYTE_SIZE = 5 * 4

/**
 * The maximum area of the bitmap used to to extract the bottom primary color.
 * As most album art are squares, this takes a base width of 320px
 * and takes the lower 1/5 of the height.
 *
 * The lower the value, the faster the primary color can be extracted from the bitmap.
 */
private const val MAX_BITMAP_AREA = 400 * 80

/**
 * The range of hue around the primary color, in which an accent color cannot be chosen.
 * The lower the value, the closer in hue a primary and an accent color can be.
 */
private const val HUE_DELTA = 15f

/**
 * Defines how a "primary color" should be selected from loaded bitmaps.
 * The primary color should be one of the most represented colors
 * and prefer moderately-saturated colors when available.
 */
private val PRIMARY_TARGET = Target.Builder()
    .setPopulationWeight(0.7f)
    .setSaturationWeight(0.3f)
    .setTargetSaturation(0.6f)
    .setLightnessWeight(0f)
    .setExclusive(false)
    .build()

/**
 * Defines how an "accent color" should be selected from loaded bitmaps.
 * The accent color should preferably be of average lightness and high saturation,
 * and be part of small, isolated parts of the image.
 */
private val ACCENT_TARGET = Target.Builder()
    .setPopulationWeight(0.1f)
    .setSaturationWeight(0.5f)
    .setLightnessWeight(0.4f)
    .setMinimumSaturation(0.2f)
    .setTargetSaturation(0.7f)
    .setMinimumLightness(0.3f)
    .setTargetLightness(0.6f)
    .setMaximumLightness(0.8f)
    .setExclusive(false)
    .build()

/**
 * Writes the given 4-bytes integer [value] in the receiver array,
 * starting at the specified [startIndex] up to [startIndex] + 3.
 *
 * Bytes are written following the Big Endian convention
 * (the most significant bytes are written first in the array).
 *
 * @receiver The array in which the integer should be written to.
 * @param startIndex Index in the array where the first byte should be written.
 * This index is expected to be less than `size - 3`.
 * @param value The integer whose 4 bytes should be written to the array.
 */
private fun ByteArray.writeInt(startIndex: Int, value: Int) {
    assert(size > startIndex + 3)
    this[startIndex] = (value ushr 24).toByte()
    this[startIndex + 1] = (value ushr 16).toByte()
    this[startIndex + 2] = (value ushr 8).toByte()
    this[startIndex + 3] = value.toByte()
}

/**
 * Extract a primary and an accent color from the provided bitmap,
 * using colors in [defaultPalette] as a fallback if any color is missing.
 *
 * @param bitmap The bitmap from which a color palette should be extracted.
 * @param defaultPalette Default colors to return in the generated palette if any is missing.
 *
 * @return The generated color palette.
 */
private fun extractColorPalette(bitmap: Bitmap, defaultPalette: AlbumPalette): AlbumPalette {
    // Generate a coarse Palette to extract the primary color from the bottom of the image.
    val primaryPalette = Palette.from(bitmap)
        .setRegion(0, 4 * bitmap.height / 5, bitmap.width, bitmap.height)
        .resizeBitmapArea(MAX_BITMAP_AREA)
        .clearFilters()
        .clearTargets()
        .addTarget(PRIMARY_TARGET)
        .generate()

    // Extracts the accent color by generating another Palette.
    // Filter out accent color too close in hue from the primary's to ensure enough contrast.
    val primaryColor = primaryPalette.getColorForTarget(PRIMARY_TARGET, defaultPalette.primary)
    val primaryColorFilter = PrimaryHueFilter(primaryColor)

    val accentPalette = Palette.from(bitmap)
        .clearTargets()
        .addTarget(ACCENT_TARGET)
        .addFilter(primaryColorFilter)
        .generate()

    val accent: Int
    val titleText: Int
    val bodyText: Int
    val textOnAccent: Int

    val primarySwatch = primaryPalette.getSwatchForTarget(PRIMARY_TARGET)
    val accentSwatch = accentPalette.getSwatchForTarget(ACCENT_TARGET)

    if (primarySwatch != null) {
        titleText = primarySwatch.titleTextColor
        bodyText = primarySwatch.bodyTextColor
    } else {
        titleText = defaultPalette.titleText
        bodyText = defaultPalette.bodyText
    }

    if (accentSwatch != null) {
        accent = accentSwatch.rgb
        textOnAccent = accentSwatch.titleTextColor
    } else {
        accent = defaultPalette.accent
        textOnAccent = defaultPalette.textOnAccent
    }

    return AlbumPalette(primaryColor, accent, titleText, bodyText, textOnAccent)
}

/**
 * The resource that will be loaded and recycled by Glide when loading an [AlbumArt].
 * Those resources can be stored in the disk cache to avoid extracting colors
 * from the loaded bitmap more than once.
 */
private class AlbumArtResource(
    private val albumPalette: AlbumPalette,
    private val bitmapResource: Resource<Bitmap>
) : Resource<AlbumArt> {
    override fun getResourceClass() = AlbumArt::class.java
    override fun get() = AlbumArt(bitmapResource.get(), albumPalette)
    override fun getSize() = bitmapResource.size + PALETTE_BYTE_SIZE
    override fun recycle() = bitmapResource.recycle()
}

/**
 * Decodes [AlbumArt]s from [ByteBuffer]s,
 * loading a Bitmap and generating the associated [AlbumPalette] from it,
 * or retrieving the palette from the cache if available.
 */
class BufferAlbumArtDecoder(
    private val bitmapDecoder: ResourceDecoder<ByteBuffer, Bitmap>
) : ResourceDecoder<ByteBuffer, AlbumArt> {

    // This Decoder is expected to decode all AlbumArt resources.
    override fun handles(source: ByteBuffer, options: Options): Boolean = true

    override fun decode(
        source: ByteBuffer,
        width: Int,
        height: Int,
        options: Options
    ): Resource<AlbumArt>? {
        // Check the last bytes for the header "ART".
        val maybeHeaderPosition = source.capacity() - (HEADER_BYTE_SIZE + PALETTE_BYTE_SIZE)
        val hasCachedPalette = source[maybeHeaderPosition] == HEADER_A
                && source[maybeHeaderPosition + 1] == HEADER_R
                && source[maybeHeaderPosition + 2] == HEADER_T

        // If the header is present, then the few last bytes are the color palette.
        // Prevent the Bitmap decoder from reading them as part of the bitmap by setting a limit.
        if (hasCachedPalette) {
            source.limit(maybeHeaderPosition)
        }

        // Delegate loading of the Bitmap to the bitmap decoder.
        val bitmapResource = bitmapDecoder.decode(source, width, height, options) ?: return null

        val palette = if (hasCachedPalette) {
            // Remove the limit, then go back to after the header bytes to read the palette colors
            source.limit(source.capacity())
            source.position(maybeHeaderPosition + HEADER_BYTE_SIZE)
            AlbumPalette(
                primary = source.getInt(),
                accent = source.getInt(),
                titleText = source.getInt(),
                bodyText = source.getInt(),
                textOnAccent = source.getInt()
            )
        } else {
            val defaultPalette = checkNotNull(options[GlideExtensions.OPTION_DEFAULT_PALETTE])
            extractColorPalette(bitmapResource.get(), defaultPalette)
        }
        return AlbumArtResource(palette, bitmapResource)
    }
}

/**
 * Decodes [AlbumArt]s from [ByteBuffer]s,
 * loading a Bitmap and generating the associated [AlbumPalette] from it.
 * This decoder is ensured to be used for all non-cached resource loads,
 * and therefore does not attempt to retrieve the generating palette from the source data stream.
 */
class StreamAlbumArtDecoder(
    private val bitmapDecoder: ResourceDecoder<InputStream, Bitmap>
) : ResourceDecoder<InputStream, AlbumArt> {

    // This Decoder is expected to decode all AlbumArt resources.
    override fun handles(source: InputStream, options: Options): Boolean = true

    override fun decode(
        source: InputStream,
        width: Int,
        height: Int,
        options: Options
    ): Resource<AlbumArt>? {

        // Load bitmap from the source
        val bitmapResource = bitmapDecoder.decode(source, width, height, options) ?: return null

        // Extract the color palette from the loaded bitmap
        val defaultPalette = checkNotNull(options[GlideExtensions.OPTION_DEFAULT_PALETTE])
        val palette = extractColorPalette(bitmapResource.get(), defaultPalette)

        return AlbumArtResource(palette, bitmapResource)
    }
}

/**
 * Write loaded [AlbumArt] to the cache, saving the generated color palette along the loaded bitmap.
 */
class AlbumArtEncoder(
    private val encoder: ResourceEncoder<Bitmap>,
    private val bitmapPool: BitmapPool,
    private val arrayPool: ArrayPool
) : ResourceEncoder<AlbumArt> {

    override fun getEncodeStrategy(options: Options) = encoder.getEncodeStrategy(options)

    override fun encode(data: Resource<AlbumArt>, file: File, options: Options): Boolean {
        val (bitmap, palette) = data.get()
        val paletteBytes = arrayPool[HEADER_BYTE_SIZE + PALETTE_BYTE_SIZE, ByteArray::class.java]

        // Write header as ASCII letters "ART".
        // This allows decoders to determine if an album art is stored in the cached file.
        paletteBytes[0] = HEADER_A
        paletteBytes[1] = HEADER_R
        paletteBytes[2] = HEADER_T

        // Write each color as a 4-bytes integer
        paletteBytes.writeInt(HEADER_BYTE_SIZE, palette.primary)
        paletteBytes.writeInt(HEADER_BYTE_SIZE + 4, palette.accent)
        paletteBytes.writeInt(HEADER_BYTE_SIZE + 2 * 4, palette.titleText)
        paletteBytes.writeInt(HEADER_BYTE_SIZE + 3 * 4, palette.bodyText)
        paletteBytes.writeInt(HEADER_BYTE_SIZE + 4 * 4, palette.textOnAccent)

        return try {
            encoder.encode(BitmapResource(bitmap, bitmapPool), file, options).also {
                FileOutputStream(file, true).use {
                    it.write(paletteBytes, 0, HEADER_BYTE_SIZE + PALETTE_BYTE_SIZE)
                }
            }
        } catch (e: IOException) {
            Timber.e(e, "Error while writing AlbumPalette to cache file.")
            false
        }
    }
}

/**
 * A Palette filter that prevents from picking an accent color
 * whose hue is too close from the selected primary color.
 *
 * @param primaryColor The selected primary color.
 */
class PrimaryHueFilter(@ColorInt primaryColor: Int) : Palette.Filter {

    /**
     * Whether the primary color is perceived as a shade of grey to the human eye.
     * The primary color will be considered a grey scale if one of the following is true:
     * - its saturation value is low,
     * - its lightness value is either very low or very high.
     */
    val primaryIsGreyScale: Boolean

    /**
     * Whether the primary color's hue is close to the origin of the color circle (i.e, red hues).
     * Due to hue values being normalized in `[0 ; 360[`, the range of forbidden accent color hues
     * has to be split into 2 ranges when `primaryHue - delta < 0` or `primaryHue + delta > 360`,
     * which typically happens when the primary hue is a shade of red.
     */
    val primaryIsNearRed: Boolean

    /**
     * The lower bound of the range of forbidden hues for the accent color.
     */
    private val lowerBound: Float

    /**
     * The higher bound of the range of forbidden hues for the accent color.
     */
    private val higherBound: Float

    init {
        // Extract HSL components from the primary color for analysis
        val (hue, sat, light) = primaryColor.toHsl()
        primaryIsGreyScale = sat < 0.2f || light !in 0.10f..0.90f
        primaryIsNearRed = hue in 0f..HUE_DELTA || hue in (360f - HUE_DELTA)..360f

        if (primaryIsNearRed) {
            lowerBound = 360f - HUE_DELTA + hue
            higherBound = hue + HUE_DELTA
        } else {
            lowerBound = hue - HUE_DELTA
            higherBound = hue + HUE_DELTA
        }
    }

    override fun isAllowed(rgb: Int, hsl: FloatArray): Boolean {
        return primaryIsGreyScale || if (primaryIsNearRed) {
            hsl[0] !in lowerBound..360f && hsl[0] !in 0f..higherBound
        } else {
            hsl[0] !in lowerBound..higherBound
        }
    }
}