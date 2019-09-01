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

package fr.nihilus.music.media.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dagger.Module
import dagger.Provides

/**
 * Provides dependencies required by the test environment such as the test [Context].
 */
@Module
internal object TestEnvironmentModule {

    @JvmStatic
    @Provides
    fun providesTestContext(): Context = ApplicationProvider.getApplicationContext()
}