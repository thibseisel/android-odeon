/*
 * Copyright 2017 Thibault Seisel
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

package fr.nihilus.music.database;

import android.arch.persistence.room.TypeConverter;
import android.net.Uri;
import android.support.annotation.Nullable;

import java.util.Date;

final class Converters {

    @TypeConverter
    public Date fromTimestamp(@Nullable Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public Long dateToTimestamp(@Nullable Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public Uri fromString(@Nullable String str) {
        // TODO Might return Uri.EMPTY instead of null
        return str == null ? null : Uri.parse(str);
    }

    @TypeConverter
    public String toUriString(@Nullable Uri uri) {
        return uri == null ? null : uri.toString();
    }

}
