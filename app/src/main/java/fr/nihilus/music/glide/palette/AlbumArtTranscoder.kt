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
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder
import fr.nihilus.music.ui.albums.AlbumPalette
import fr.nihilus.music.utils.toHsl
import timber.log.Timber

data class AlbumArt(val bitmap: Bitmap, val palette: AlbumPalette)

class AlbumArtResource(
    private val albumPalette: AlbumPalette,
    private val bitmapResource: Resource<Bitmap>
) : Resource<AlbumArt> {

    override fun getResourceClass() = AlbumArt::class.java

    override fun get() = AlbumArt(bitmapResource.get(), albumPalette)

    // Size of the bitmap + 5 color integers (4 bytes each)
    override fun getSize() = bitmapResource.size + ALBUM_PALETTE_BYTE_SIZE

    override fun recycle() {
        bitmapResource.recycle()
    }

    companion object {
        const val ALBUM_PALETTE_BYTE_SIZE = 5 * 4
    }
}

/**
 * The maximum area of the bitmap used to to extract the bottom primary color.
 * As most album art are squares, this takes a base width of 320px
 * and takes the lower 1/5 of the height.
 */
private const val MAX_BITMAP_AREA = 400 * 80

private val PRIMARY_TARGET = Target.Builder()
    .setPopulationWeight(0.7f)
    .setSaturationWeight(0.3f)
    .setTargetSaturation(0.6f)
    .setLightnessWeight(0f)
    .setExclusive(false)
    .build()

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


class AlbumArtTranscoder(context: Context) : ResourceTranscoder<Bitmap, AlbumArt> {

    private val defaultPalette = AlbumColorModule.providesDefaultAlbumPalette(context)

    override fun transcode(toTranscode: Resource<Bitmap>, options: Options): Resource<AlbumArt> {
        Timber.d("Transcoding to AlbumArt")
        val bitmap = toTranscode.get()

        // Generate a coarse Palette to extract the dominant color from the bottom of the image.
        val primaryPalette = Palette.from(bitmap)
            .setRegion(0, 4 * bitmap.height / 5, bitmap.width, bitmap.height)
            .resizeBitmapArea(MAX_BITMAP_AREA)
            .clearFilters()
            .clearTargets()
            .addTarget(PRIMARY_TARGET)
            .generate()


        val primaryColor = primaryPalette.getColorForTarget(PRIMARY_TARGET, defaultPalette.primary)
        val primaryColorFilter = PrimaryHueFilter(primaryColor)

        // Generate a Palette to extract the color tu use as the accent color.
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

        val colorPack = AlbumPalette(primaryColor, accent, titleText, bodyText, textOnAccent)
        return AlbumArtResource(colorPack, toTranscode)
    }
}

private const val HUE_DELTA = 15f

/**
 *
 */
internal class PrimaryHueFilter(@ColorInt primaryColor: Int) : Palette.Filter {

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