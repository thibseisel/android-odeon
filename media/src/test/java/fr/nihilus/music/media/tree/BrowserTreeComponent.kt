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

package fr.nihilus.music.media.tree

import dagger.BindsInstance
import dagger.Component
import fr.nihilus.music.media.database.AppDatabase
import fr.nihilus.music.media.database.InMemoryDatabaseModule
import fr.nihilus.music.media.di.MediaSourceModule
import fr.nihilus.music.media.di.ServiceScoped
import fr.nihilus.music.media.di.TestEnvironmentModule
import fr.nihilus.music.media.di.TestExecutionContextModule
import fr.nihilus.music.media.os.SimulatedSystemModule
import io.reactivex.schedulers.TestScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestCoroutineDispatcher
import javax.inject.Named

@ServiceScoped
@Component(
    modules = [
        TestEnvironmentModule::class,
        TestExecutionContextModule::class,
        SimulatedSystemModule::class,
        InMemoryDatabaseModule::class,
        MediaSourceModule::class
    ]
)
internal interface BrowserTreeComponent {
    val dispatcher: TestCoroutineDispatcher
    val scheduler: TestScheduler
    val database: AppDatabase

    fun createBrowserTree(): BrowserTree

    @Component.Builder
    interface Builder {
        @BindsInstance fun runningIn(scope: CoroutineScope): Builder
        @BindsInstance fun startingAtTime(@Named("TestClock.startTime") startTime: Long): Builder

        fun build(): BrowserTreeComponent
    }
}