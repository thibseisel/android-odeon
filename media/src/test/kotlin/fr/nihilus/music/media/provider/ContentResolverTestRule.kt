/*
 * Copyright 2022 Thibault Seisel
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

package fr.nihilus.music.media.provider

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.Context
import android.content.pm.ProviderInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.robolectric.Robolectric
import org.robolectric.android.controller.ContentProviderController

/**
 * JUnit test rule to test code that use [ContentResolver].
 */
internal class ContentResolverTestRule : TestWatcher() {
    private var controller: ContentProviderController<out ContentProvider>? = null

    /**
     * Instance of the [ContentResolver] to be used in tests.
     */
    val resolver = getContentResolver()

    /**
     * Associate a [ContentProvider] to the given [providerAuthority], replacing any existing
     * provider already associated with that authority.
     * @param providerAuthority Authority to be associated with the content provider instance.
     * @param contentProviderClass Content provider class, generally a provider test double.
     */
    fun registerProvider(
        providerAuthority: String,
        contentProviderClass: Class<out ContentProvider>
    ) {
        if (controller != null) {
            error("Only one provider can be registered at the same time")
        }
        controller = Robolectric.buildContentProvider(contentProviderClass).create(
            ProviderInfo().apply { authority = providerAuthority }
        )
    }

    /**
     * Retrieves an instance of the [ContentProvider] that have been registered
     * with [registerProvider].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : ContentProvider> getProvider(): T {
        val provider = controller?.get() ?: error("No provider has been registered")
        return provider as T
    }

    override fun finished(description: Description?) {
        controller?.shutdown()
    }
}

private fun getContentResolver(): ContentResolver {
    val application = ApplicationProvider.getApplicationContext<Context>()
    return application.contentResolver
}
