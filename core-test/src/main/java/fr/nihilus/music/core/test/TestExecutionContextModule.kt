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

package fr.nihilus.music.core.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dagger.Module
import dagger.Provides
import fr.nihilus.music.core.context.AppDispatchers
import fr.nihilus.music.core.context.RxSchedulers
import io.reactivex.schedulers.TestScheduler
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import javax.inject.Singleton

/**
 * Provides an execution context for testing code using RxJava and Kotlin Coroutines.
 * This module only uses a single execution context for all cases.
 *
 * Use this module as a test alternative to [ExecutionContextModule][fr.nihilus.music.core.context.ExecutionContextModule].
 *
 * Execution of RxJava operators and coroutines can be controlled from test code
 * with the [TestScheduler] and [TestCoroutineDispatcher] provided with this module.
 */
@Module
internal object TestExecutionContextModule {

    @Provides
    fun providesTestApplicationContext(): Context = ApplicationProvider.getApplicationContext()

    @Provides @Singleton
    fun providesTestScheduler() = TestScheduler()

    @Provides @Singleton
    fun providesTestingSchedulers(scheduler: TestScheduler) = RxSchedulers(scheduler)

    @Provides @Singleton
    fun providesTestDispatcher() = TestCoroutineDispatcher()

    @Provides @Singleton
    fun providesTestingDispatchers(dispatcher: TestCoroutineDispatcher) = AppDispatchers(dispatcher)

    @Provides @Singleton
    fun providesTestCoroutineScope(dispatcher: TestCoroutineDispatcher) = TestCoroutineScope(dispatcher)
}