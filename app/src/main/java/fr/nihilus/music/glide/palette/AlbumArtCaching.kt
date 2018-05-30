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

import android.content.Context
import android.graphics.Bitmap
import android.support.annotation.ColorInt
import android.support.v7.graphics.Palette
import android.support.v7.graphics.Target
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.ResourceEncoder
import com.bumptech.glide.load.data.BufferedOutputStream
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import fr.nihilus.music.ui.albums.AlbumPalette
import fr.nihilus.music.utils.toHsl
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

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
 */
private const val MAX_BITMAP_AREA = 400 * 80

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

private fun ByteArray.setInt(startIndex: Int, value: Int) {
    assert(size > startIndex + 3)
    this[startIndex] = (value ushr 24).toByte()
    this[startIndex + 1] = (value ushr 16).toByte()
    this[startIndex + 2] = (value ushr 8).toByte()
    this[startIndex + 3] = value.toByte()
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
    context: Context,
    private val bitmapDecoder: ResourceDecoder<ByteBuffer, Bitmap>
) : ResourceDecoder<ByteBuffer, AlbumArt> {

    private val defaultPalette = AlbumColorModule.providesDefaultAlbumPalette(context)

    // This Decoder is expected to decode all AlbumArt resources.
    override fun handles(source: ByteBuffer, options: Options): Boolean = true

    override fun decode(
        source: ByteBuffer,
        width: Int,
        height: Int,
        options: Options
    ): Resource<AlbumArt>? {
        // Read the first 3 bytes to check if it matches the ASCII sequence "ART"
        val hasCachedPalette = source.get(0) == HEADER_A
                && source.get(1) == HEADER_R
                && source.get(2) == HEADER_T

        // Advance source cursor to skip the cached palette headers and cached colors
        if (hasCachedPalette) source.position(HEADER_BYTE_SIZE + PALETTE_BYTE_SIZE)

        // Delegate loading of the Bitmap to the passed decoder
        val bitmapResource = bitmapDecoder.decode(source, width, height, options) ?: return null

        val palette: AlbumPalette = if (hasCachedPalette) {
            Timber.d("Palette is already in cache. Retrieving colors.")
            // Go back to after the header bytes to read the palette colors
            source.position(3)
            AlbumPalette(
                primary = source.getInt(),
                accent = source.getInt(),
                titleText = source.getInt(),
                bodyText = source.getInt(),
                textOnAccent = source.getInt()
            )

        } else extractPalette(bitmapResource.get())

        return AlbumArtResource(palette, bitmapResource)
    }

    private fun extractPalette(bitmap: Bitmap): AlbumPalette {
        Timber.d("Extracting colors from Bitmap")

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
}

class AlbumArtEncoder(
    private val encoder: ResourceEncoder<Bitmap>,
    private val bitmapPool: BitmapPool,
    private val arrayPool: ArrayPool
) : ResourceEncoder<AlbumArt> {

    override fun getEncodeStrategy(options: Options) = encoder.getEncodeStrategy(options)

    override fun encode(data: Resource<AlbumArt>, file: File, options: Options): Boolean {
        Timber.d("Encoding AlbumArt to file %s", file.name)
        val (bitmap, palette) = data.get()
        val paletteBytes = arrayPool[HEADER_BYTE_SIZE + PALETTE_BYTE_SIZE, ByteArray::class.java]

        // Write header as ASCII letters "ART"
        paletteBytes[0] = HEADER_A
        paletteBytes[1] = HEADER_R
        paletteBytes[2] = HEADER_T

        // Write each color as a 4-bytes integer
        paletteBytes.setInt(HEADER_BYTE_SIZE, palette.primary)
        paletteBytes.setInt(HEADER_BYTE_SIZE + 4, palette.accent)
        paletteBytes.setInt(HEADER_BYTE_SIZE + 2 * 4, palette.titleText)
        paletteBytes.setInt(HEADER_BYTE_SIZE + 3 * 4, palette.bodyText)
        paletteBytes.setInt(HEADER_BYTE_SIZE + 4 * 4, palette.textOnAccent)

        try {
            BufferedOutputStream(file.outputStream(), arrayPool).use {
                it.write(paletteBytes, 0, HEADER_BYTE_SIZE + PALETTE_BYTE_SIZE)
            }
        } catch (e: IOException) {
            Timber.e(e, "Error while writing AlbumPalette to cache file.")
        }

        return encoder.encode(BitmapResource(bitmap, bitmapPool), file, options)
    }
}

private const val HUE_DELTA = 15f

private class PrimaryHueFilter(@ColorInt primaryColor: Int) :
    Palette.Filter {

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
        primaryIsNearRed = hue in 0f..
                HUE_DELTA || hue in (360f - HUE_DELTA)..360f

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