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
import android.support.v4.content.ContextCompat
import android.support.v7.graphics.Palette
import android.support.v7.graphics.Target
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder
import com.bumptech.glide.util.Util
import fr.nihilus.music.R
import fr.nihilus.music.ui.albums.AlbumPalette

data class AlbumArt(val bitmap: Bitmap, val palette: AlbumPalette)

class AlbumArtResource(
    private val albumArt: AlbumArt,
    private val bitmapPool: BitmapPool
) : Resource<AlbumArt> {

    override fun getResourceClass() = AlbumArt::class.java

    override fun get() = albumArt

    // Size of the bitmap + 4 color integers (4 bytes each)
    override fun getSize() = Util.getBitmapByteSize(albumArt.bitmap) + (4 * 4)

    override fun recycle() {
        bitmapPool.put(albumArt.bitmap)
    }
}

/**
 * The maximum area of the bitmap used to to extract the bottom primary color.
 * As most album art are squares, this takes a base width of 320px
 * and takes the lower 1/5 of the height.
 */
private const val MAX_BITMAP_AREA = 400 * 80

private val DOMINANT_TARGET = Target.Builder()
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
    .setMinimumLightness(0.4f)
    .setTargetLightness(0.5f)
    .setMaximumLightness(0.9f)
    .setExclusive(false)
    .build()


class AlbumArtTranscoder(
    context: Context,
    private val bitmapPool: BitmapPool
) : ResourceTranscoder<Bitmap, AlbumArt> {

    private val defaultPalette = AlbumPalette(
        primary = ContextCompat.getColor(context, R.color.album_band_default),
        accent = ContextCompat.getColor(context, R.color.color_accent),
        titleText = ContextCompat.getColor(context, android.R.color.white),
        bodyText = ContextCompat.getColor(context, android.R.color.white)
    )

    override fun transcode(toTranscode: Resource<Bitmap>, options: Options): Resource<AlbumArt> {
        val bitmap = toTranscode.get()

        // Generate a coarse Palette to extract the dominant color from the bottom quarter.
        val primaryColorPalette = Palette.from(bitmap)
            .setRegion(0, 4 * bitmap.height / 5, bitmap.width, bitmap.height)
            .resizeBitmapArea(MAX_BITMAP_AREA)
            .clearFilters()
            .clearTargets()
            .addTarget(DOMINANT_TARGET)
            .generate()

        // TODO Maybe better to create a single Target that matches all vibrant colors
        // Generate a precise Palette to extract the color tu use as accent.
        val accentColorPalette = Palette.from(bitmap)
            .clearTargets()
            .addTarget(ACCENT_TARGET)
            .generate()

        val accentColor = accentColorPalette.getColorForTarget(ACCENT_TARGET, defaultPalette.accent)
        val colorPack = primaryColorPalette.dominantSwatch?.let {
            AlbumPalette(
                primary = it.rgb,
                accent = accentColor,
                titleText = it.titleTextColor,
                bodyText = it.bodyTextColor
            )
        } ?: defaultPalette.copy(accent = accentColor)

        val albumArt = AlbumArt(bitmap, colorPack)
        return AlbumArtResource(albumArt, bitmapPool)
    }
}