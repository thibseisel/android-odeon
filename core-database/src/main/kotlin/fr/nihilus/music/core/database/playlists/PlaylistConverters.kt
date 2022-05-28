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

package fr.nihilus.music.core.database.playlists

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.TypeConverter

internal class PlaylistConverters {

    @TypeConverter
    fun fromString(str: String?): Uri? = str?.toUri()

    @TypeConverter
    fun toUriString(uri: Uri?): String? = uri?.toString()
}
