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

package fr.nihilus.music.media

import android.net.Uri
import android.support.v4.media.MediaDescriptionCompat
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertNull
import org.junit.Assert.fail
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

    assertThat(descr.mediaId).isEqualTo(mediaId)
    assertThat(descr.title).isEqualTo(title)
    assertThat(descr.subtitle).isEqualTo(subtitle)
    assertThat(descr.description).isEqualTo(description)
    assertThat(descr.iconUri).isEqualTo(iconUri)
    assertThat(descr.mediaUri).isEqualTo(mediaUri)

    if (extras != null && descr.extras != null) {
        assertThat(descr.extras?.size() ?: 0).isEqualTo(extras.size)
        for ((key, value) in extras) {
            assertThat(descr.extras!!.get(key)).isEqualTo(value)
        }
    } else {
        assertNull(descr.extras)
    }
}

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