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

package fr.nihilus.music.common.test

import dagger.BindsInstance
import dagger.Component
import fr.nihilus.music.common.CoreComponent
import fr.nihilus.music.common.test.database.InMemoryDatabaseModule
import fr.nihilus.music.common.test.os.RevocablePermission
import fr.nihilus.music.common.test.os.TestClock
import io.reactivex.schedulers.TestScheduler
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
    /*override */val appScope: TestCoroutineScope
    val dispatcher: TestCoroutineDispatcher
    val scheduler: TestScheduler

    @Component.Builder
    interface Builder {
        fun clockTime(@BindsInstance @Named("TestClock.startTime") timeMillis: Long?): Builder
        fun create(): CoreTestComponent
    }
}