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

package fr.nihilus.music.core.ui.glide.palette

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.ColorInt
import fr.nihilus.music.core.ui.extensions.darker

data class AlbumPalette(
    @ColorInt val primary: Int,
    @ColorInt val accent: Int,
    @ColorInt val titleText: Int,
    @ColorInt val bodyText: Int,
    @ColorInt val textOnAccent: Int
) : Parcelable {
    @ColorInt val primaryDark = darker(primary, 0.8f)

    constructor(parcel: Parcel) : this(
        primary = parcel.readInt(),
        accent = parcel.readInt(),
        titleText = parcel.readInt(),
        bodyText = parcel.readInt(),
        textOnAccent = parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(primary)
        parcel.writeInt(accent)
        parcel.writeInt(titleText)
        parcel.writeInt(bodyText)
        parcel.writeInt(textOnAccent)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<AlbumPalette> {
        override fun createFromParcel(parcel: Parcel): AlbumPalette = AlbumPalette(parcel)
        override fun newArray(size: Int): Array<AlbumPalette?> = arrayOfNulls(size)
    }
}