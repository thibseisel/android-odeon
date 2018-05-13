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

import android.os.Parcel
import android.os.Parcelable
import android.support.v7.graphics.Palette

/**
 * Creates a copy of a palette that can be written to a [Parcel].
 * @receiver A Palette to be written to a Parcel.
 * @return A copy of the palette instance that is parcelable.
 */
fun Palette.toParcelable() = ParcelablePalette(swatches)

/**
 * A wrapper around [Palette] properties in order to read from and written to an Android [Parcel].
 * This is useful to pass a generated palette to another system component via a Bundle or an Intent.
 */
class ParcelablePalette(
    private val swatches: List<Palette.Swatch>
) : Parcelable {

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(swatches.size)
        swatches.forEach {
            dest.writeInt(it.rgb)
            dest.writeInt(it.population)
        }
    }

    override fun describeContents(): Int = 0

    /**
     * Recreates a platform Palette instance from this parcelable Palette.
     * @return A Palette instance that is not parcelable.
     */
    fun asPalette() = Palette.Builder(swatches).generate()

    companion object CREATOR : Parcelable.Creator<ParcelablePalette> {
        override fun newArray(size: Int) = arrayOfNulls<ParcelablePalette>(size)

        override fun createFromParcel(source: Parcel): ParcelablePalette {
            return ParcelablePalette(List(source.readInt()) {
                Palette.Swatch(
                    source.readInt(),
                    source.readInt()
                )
            })
        }
    }
}