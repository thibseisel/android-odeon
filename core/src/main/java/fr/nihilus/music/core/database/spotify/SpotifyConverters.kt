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

package fr.nihilus.music.core.database.spotify

import androidx.room.TypeConverter

internal class SpotifyConverters {

    @TypeConverter
    fun MusicalMode.toEncodedMode(): Int = when (this) {
        MusicalMode.MINOR -> 0
        MusicalMode.MAJOR -> 1
    }

    @TypeConverter
    fun Int.toMusicalMode(): MusicalMode = decodeMusicalMode(this)

    @TypeConverter
    fun Pitch?.toEncodedKey(): Int? = when (this) {
        Pitch.C -> 0
        Pitch.D_FLAT -> 1
        Pitch.D -> 2
        Pitch.E_FLAT -> 3
        Pitch.E -> 4
        Pitch.F -> 5
        Pitch.F_SHARP -> 6
        Pitch.G -> 7
        Pitch.A_FLAT -> 8
        Pitch.A -> 9
        Pitch.B_FLAT -> 10
        Pitch.B -> 11
        else -> null
    }

    @TypeConverter
    fun Int?.toPitchKey(): Pitch? = decodePitch(this)
}
