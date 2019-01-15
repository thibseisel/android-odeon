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

package fr.nihilus.music.media.database

import android.net.Uri
import androidx.room.TypeConverter
import fr.nihilus.music.media.toUri
import java.util.*

internal class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?) = if (value != null) Date(value) else null

    @TypeConverter
    fun dateToTimestamp(date: Date?) = date?.time

    @TypeConverter
    fun fromString(str: String?): Uri = str?.toUri() ?: Uri.EMPTY

    @TypeConverter
    fun toUriString(uri: Uri?) = uri?.toString()

}
