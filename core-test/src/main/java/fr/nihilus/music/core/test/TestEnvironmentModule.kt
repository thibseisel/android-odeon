/*
 * Copyright 2020 Thibault Seisel
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

package fr.nihilus.music.core.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestCoroutineScope

/**
 * Provides dependencies required by the test environment such as the test [Context].
 */
@Module(includes = [TestExecutionContextModule::class])
abstract class TestEnvironmentModule {

    @Binds
    abstract fun bindsTestCoroutineScope(testScope: TestCoroutineScope): CoroutineScope

    companion object {

        @Provides
        fun providesTestContext(): Context = ApplicationProvider.getApplicationContext()
    }
}