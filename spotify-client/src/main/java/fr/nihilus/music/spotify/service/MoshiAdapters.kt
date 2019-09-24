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

package fr.nihilus.music.spotify.service

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import fr.nihilus.music.spotify.model.MusicalMode
import fr.nihilus.music.spotify.model.Pitch
import fr.nihilus.music.spotify.model.Pitch.*

internal class MusicalModeAdapter : JsonAdapter<MusicalMode>() {

    override fun fromJson(reader: JsonReader): MusicalMode? {
        return if (reader.peek() == JsonReader.Token.NULL) {
            reader.nextNull()
        } else {
            when (reader.nextInt()) {
                0 -> MusicalMode.MINOR
                1 -> MusicalMode.MAJOR
                else -> null
            }
        }
    }

    override fun toJson(writer: JsonWriter, mode: MusicalMode?) {
        if (mode == null) {
            writer.nullValue()
        } else {
            writer.value(when (mode) {
                MusicalMode.MINOR -> 0
                MusicalMode.MAJOR -> 1
            })
        }
    }
}

internal class PitchAdapter : JsonAdapter<Pitch?>() {

    override fun toJson(writer: JsonWriter, pitch: Pitch?) {
        if (pitch == null) {
            writer.nullValue()
        } else {
            writer.value(pitch.ordinal)
        }
    }

    override fun fromJson(reader: JsonReader): Pitch? {
        if (reader.peek() == JsonReader.Token.NULL) {
            return reader.nextNull()
        }

        return when (reader.nextInt()) {
            0 -> C
            1 -> C_SHARP
            2 -> D
            3 -> D_SHARP
            4 -> E
            5 -> F
            6 -> F_SHARP
            7 -> G
            8 -> G_SHARP
            9 -> A
            10 -> A_SHARP
            11 -> B
            else -> null
        }
    }
}