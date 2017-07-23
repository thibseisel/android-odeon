package fr.nihilus.mymusic.media;

import android.app.Application;
import android.content.ContentProvider;
import android.database.MatrixCursor;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Media;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.media.MediaMetadataCompat;
import android.test.mock.MockContentResolver;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import fr.nihilus.mymusic.media.mock.MockCursorProvider;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class MediaDaoTest {

    private static final Object[][] VALUES = {
            {1L, "Title", "Album", "Artist", 123, 101, "TitleKey", "AlbumKey", 1L, 1L, ""},
            {2L, "Amerika", "Reise Reise", "Rammstein", 3046, 106, "Amerika", "ReiseReise", 2L, 2L, ""},
            {42L, "Fever", "Fever", "Bullet For My Valentine", 2567, 101, "Fever", "Fever", 3L, 3L, ""}
    };

    private MediaDao testSubject;
    private MatrixCursor cursor;

    @Before
    public void setUp() throws Exception {
        cursor = getMockCursor();
        ContentProvider mockProvider = new MockCursorProvider(cursor);

        Application app = mock(Application.class);
        MockContentResolver mockResolver = new MockContentResolver();
        when(app.getContentResolver()).thenReturn(mockResolver);
        mockResolver.addProvider(MediaStore.AUTHORITY, mockProvider);

        testSubject = new MediaDao(app);
    }

    private static MatrixCursor getMockCursor() {
        String[] columns = {BaseColumns._ID, Media.TITLE, Media.ALBUM,
                Media.ARTIST, Media.DURATION, Media.TRACK, Media.TITLE_KEY, Media.ALBUM_KEY,
                Media.ALBUM_ID, Media.ARTIST_ID, Media.DATA};

        MatrixCursor cursor = new MatrixCursor(columns);
        for (Object[] row : VALUES) {
            cursor.addRow(row);
        }

        return cursor;
    }

    @Test
    @SmallTest
    public void allTracks_cursorToMetadata() throws Exception {
        List<MediaMetadataCompat> data = testSubject.getAllTracks().firstElement().blockingGet();

        assertThat(data, hasSize(VALUES.length));
        assertMetadataHas(data.get(0), "1", "Title", "Album", "Artist", 123L, 1L, 1L, artUriOf(1));
        assertMetadataHas(data.get(1), "2", "Amerika", "Reise Reise", "Rammstein", 3046L, 1L, 6L, artUriOf(2));
    }

    private void assertMetadataHas(MediaMetadataCompat meta, String mediaId, String title,
                                   String album, String artist, long duration, long discNo,
                                   long trackNo, String artUri) {
        assertEquals(mediaId, meta.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID));
        assertEquals(title, meta.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
        assertEquals(album, meta.getString(MediaMetadataCompat.METADATA_KEY_ALBUM));
        assertEquals(artist, meta.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
        assertEquals(duration, meta.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
        assertEquals(discNo, meta.getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER));
        assertEquals(trackNo, meta.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER));
        assertEquals(artUri, meta.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI));
    }

    private static String artUriOf(long musicId) {
        return "content://media/external/audio/albumart/" + musicId;
    }
}
