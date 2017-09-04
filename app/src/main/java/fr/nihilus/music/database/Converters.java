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
