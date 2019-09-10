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
import fr.nihilus.music.media.os.SimulatedSystemModule
import fr.nihilus.music.media.provider.MediaStoreSurrogate
import io.reactivex.schedulers.TestScheduler
import kotlinx.coroutines.test.TestCoroutineScope
import javax.inject.Named

/**
 * A Dagger component used to build all required dependencies
 * for testing integration of [BrowserTree].
 */
@ServiceScoped
@Component(
    modules = [
        TestEnvironmentModule::class,
        SimulatedSystemModule::class,
        InMemoryDatabaseModule::class,
        MediaSourceModule::class
    ]
)
internal interface BrowserTreeComponent {

    /**
     * A reference to the scope in which all coroutines are run.
     * Can be used to manually control coroutine execution during test.
     *
     * That scope should be [cleaned up][TestCoroutineScope.cleanupTestCoroutines] after each test.
     */
    val scope: TestCoroutineScope

    /**
     * The RxJava scheduler used by all dependencies.
     * Can be used to manually advance time in RxJava chains.
     */
    val scheduler: TestScheduler

    /**
     * A reference to the test Room database.
     * That database should be [closed][AppDatabase.close] after each test.
     */
    val database: AppDatabase

    /**
     * A reference to the test MediaStore database.
     * This should be [released][MediaStoreSurrogate.release] after each test.
     */
    val inMemoryMediaStore: MediaStoreSurrogate

    /**
     * Create the instance of the [BrowserTree] under test,
     * creating all its dependencies at the same time.
     */
    fun createBrowserTree(): BrowserTree

    /**
     * Allows to configure parameters of the [BrowserTreeComponent].
     */
    @Component.Builder
    interface Builder {

        /**
         * Specify an optional Unix timestamp to be used as the current time
         * at which test are running.
         *
         * @param startTime The time at which the fake clock should be initialized,
         * expressed as the number of milliseconds since the 1st of January 1970, 00:00 UTC.
         */
        @BindsInstance
        fun startingAtTime(@Named("TestClock.startTime") startTime: Long?): Builder

        /**
         * Create the component.
         */
        fun build(): BrowserTreeComponent
    }
}