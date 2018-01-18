package fr.nihilus.music

import android.net.Uri
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.*
import org.mockito.Mockito

fun assertMediaDescription(
    descr: MediaDescriptionCompat,
    mediaId: String? = null,
    title: CharSequence? = null,
    subtitle: CharSequence? = null,
    description: CharSequence? = null,
    iconUri: Uri? = null,
    mediaUri: Uri? = null,
    extras: Map<String, Any>? = null
) {

    assertThat(descr.mediaId, equalTo(mediaId))
    assertThat(descr.title, equalTo(title))
    assertThat(descr.subtitle, equalTo(subtitle))
    assertThat(descr.description, equalTo(description))
    assertThat(descr.iconUri, equalTo(iconUri))
    assertThat(descr.mediaUri, equalTo(mediaUri))

    if (extras != null && descr.extras != null) {
        assertThat(descr.extras?.size() ?: 0, `is`(extras.size))
        for ((key, value) in extras) {
            assertThat(descr.extras!!.get(key), equalTo(value))
        }
    } else {
        assertNull(descr.extras)
    }
}

fun assertMetadataKeyEquals(
    expected: MediaMetadataCompat,
    actual: MediaMetadataCompat,
    key: String
) =
    assertEquals(expected.bundle.get(key), actual.bundle.get(key))

/**
 * Assert that the following block function throws a given [Exception].
 *
 * @param T The type of the expected exception
 */
inline fun <reified T : Throwable> assertThrows(block: () -> Unit) {
    try {
        block()
        fail("An ${T::class.java.name} should have been thrown")
    } catch (thr: Throwable) {
        if (thr !is T) {
            fail("Unexpected exception: $thr")
        }
    }
}

/**
 * Helper function to mock objects using Mockito with a more readable syntax.
 */
inline fun <reified T> mock(): T = Mockito.mock(T::class.java)