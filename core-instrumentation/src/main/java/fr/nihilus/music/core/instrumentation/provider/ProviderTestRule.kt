/*
 * Copyright 2021 Thibault Seisel
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

package fr.nihilus.music.core.instrumentation.provider

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.pm.ProviderInfo
import android.test.mock.MockContentResolver
import androidx.test.platform.app.InstrumentationRegistry
import fr.nihilus.music.core.instrumentation.provider.ProviderTestRule.Companion.create
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.lang.ref.WeakReference
import androidx.test.rule.provider.ProviderTestRule as AndroidXProviderTestRule

/**
 * An alternative implementation of [AndroidXProviderTestRule] that creates the [ContentProvider]
 * under test with a real context, allowing Hilt to resolve the application component.
 *
 * Use [create] to create a new instance.
 */
class ProviderTestRule private constructor(
    private val providerRef: WeakReference<ContentProvider>,
    val resolver: ContentResolver
) : TestRule {

    override fun apply(base: Statement, description: Description?): Statement =
        ProviderStatement(base)

    private inner class ProviderStatement(private val base: Statement) : Statement() {
        override fun evaluate() {
            try {
                base.evaluate()
            } finally {
                providerRef.get()?.shutdown()
            }
        }
    }

    companion object {
        fun create(
            providerClass: Class<out ContentProvider>,
            providerAuthority: String
        ): ProviderTestRule {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val provider = providerClass.newInstance()
            val providerInfo = ProviderInfo()
            providerInfo.authority = providerAuthority
            provider.attachInfo(context, providerInfo)

            val resolver = MockContentResolver()
            resolver.addProvider(providerAuthority, provider)

            return ProviderTestRule(WeakReference(provider), resolver)
        }
    }
}
