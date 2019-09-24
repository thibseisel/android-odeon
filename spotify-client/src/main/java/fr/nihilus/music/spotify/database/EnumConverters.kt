/*
 * Copyright 2019 Thibault Seisel
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

package fr.nihilus.music.spotify.database

import androidx.room.TypeConverter
import fr.nihilus.music.spotify.model.MusicalMode
import fr.nihilus.music.spotify.model.Pitch

class EnumConverters {

    @TypeConverter
    internal fun encodeMusicalMode(mode: MusicalMode): Int = when (mode) {
        MusicalMode.MINOR -> 0
        MusicalMode.MAJOR -> 1
    }

    @TypeConverter
    internal fun decodeMusicalMode(mode: Int): MusicalMode = when (mode) {
        0 -> MusicalMode.MINOR
        1 -> MusicalMode.MAJOR
        else -> error("Invalid encoded value for MusicalMode: $mode")
    }

    @TypeConverter
    internal fun encodePitch(key: Pitch?): Int? = when (key) {
        Pitch.C -> 0
        Pitch.C_SHARP -> 1
        Pitch.D -> 2
        Pitch.D_SHARP -> 3
        Pitch.E -> 4
        Pitch.F -> 5
        Pitch.F_SHARP -> 6
        Pitch.G -> 7
        Pitch.G_SHARP -> 8
        Pitch.A -> 9
        Pitch.A_SHARP -> 10
        Pitch.B -> 11
        else -> null
    }

    @TypeConverter
    internal fun decodePitch(encodedKey: Int?): Pitch? = when (encodedKey) {
        0 -> Pitch.C
        1 -> Pitch.C_SHARP
        2 -> Pitch.D
        3 -> Pitch.D_SHARP
        4 -> Pitch.E
        5 -> Pitch.F
        6 -> Pitch.F_SHARP
        7 -> Pitch.G
        8 -> Pitch.G_SHARP
        9 -> Pitch.A
        10 -> Pitch.A_SHARP
        11 -> Pitch.B
        else -> null
    }
}
