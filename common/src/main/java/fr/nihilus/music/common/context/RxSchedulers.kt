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

package fr.nihilus.music.common.context

import io.reactivex.Scheduler
import org.jetbrains.annotations.TestOnly

/**
 * A group of RxJava's [Scheduler]s to be used in specific execution contexts.
 * Unlike [io.reactivex.schedulers.Schedulers], this allows changing the schedulers
 * used by application components at compile time, using different ones for production and tests.
 *
 * @constructor Create a new group of schedulers.
 *
 * @param Main Scheduler for the thread dedicated to the UI.
 * @param Default Pool of threads dedicated to CPU-intensive tasks.
 * @param IO Pool of threads dedicated to blocking IO.
 * @param Database Scheduler for reading/writing to a database.
 * This differentiates from [IO] in that it operates on a single thread.
 */
class RxSchedulers(
    val Main: Scheduler,
    val Default: Scheduler,
    val IO: Scheduler,
    val Database: Scheduler
) {
    /**
     * Create a group consisting of a single scheduler for testing.
     * This comes handy when used with [io.reactivex.schedulers.TestScheduler] confine execution
     * of test code to a single thread, avoiding concurrency problems.
     *
     * @param scheduler The scheduler to be used in all contexts.
     */
    @TestOnly
    constructor(scheduler: Scheduler) : this(scheduler, scheduler, scheduler, scheduler)
}