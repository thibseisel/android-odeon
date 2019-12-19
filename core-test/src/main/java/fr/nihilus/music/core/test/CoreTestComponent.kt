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

import dagger.BindsInstance
import dagger.Component
import fr.nihilus.music.core.CoreComponent
import fr.nihilus.music.core.test.database.InMemoryDatabaseModule
import fr.nihilus.music.core.test.os.RevocablePermission
import fr.nihilus.music.core.test.os.TestClock
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(modules = [
    CommonTestModule::class,
    TestExecutionContextModule::class,
    InMemoryDatabaseModule::class
])
interface CoreTestComponent : CoreComponent {
    override val permissions: RevocablePermission
    override val clock: TestClock
    val appScope: TestCoroutineScope

    @Component.Builder
    interface Builder {

        /**
         * Sets the test dispatcher to use for immediate execution of coroutines.
         */
        fun dispatcher(@BindsInstance dispatcher: TestCoroutineDispatcher): Builder

        /**
         * Specify the optional instant at which the clock should be initialized,
         * expressed as the number of seconds since January 1st 1970 00:00:00.
         *
         * If not set, this defaults to January 1st 1970 00:00:00.
         */
        fun clockTime(@BindsInstance @Named("TestClock.startTime") timeMillis: Long?): Builder

        /**
         * Create the test component.
         */
        fun create(): CoreTestComponent
    }
}