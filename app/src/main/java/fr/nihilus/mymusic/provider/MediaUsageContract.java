package fr.nihilus.mymusic.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public interface MediaUsageContract {

    String AUTHORITY = "nihilus.mymusic.database";
    Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    String TABLE_NAME = "usage";

    String _ID = BaseColumns._ID;
    String COL_READ_COUNT = "read_count";
}
