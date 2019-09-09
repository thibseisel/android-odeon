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

package fr.nihilus.music.media

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.test.ext.truth.os.BundleSubject
import com.google.common.truth.Correspondence
import com.google.common.truth.Truth

/**
 * Allows Truth assertions to compare media items by their media id.
 */
internal val THEIR_MEDIA_ID = Correspondence.transforming<MediaBrowserCompat.MediaItem?, String?>(
    { it?.mediaId },
    "has a media id of"
)

internal inline fun assertOn(extras: Bundle?, assertions: BundleSubject.() -> Unit) {
    Truth.assertThat(extras).named("extras").isNotNull()
    BundleSubject.assertThat(extras).run(assertions)
}