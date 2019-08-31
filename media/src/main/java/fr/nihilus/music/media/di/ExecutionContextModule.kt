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

import dagger.Module
import dagger.Provides
import dagger.Reusable
import fr.nihilus.music.media.AppDispatchers
import fr.nihilus.music.media.RxSchedulers
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers

/**
 * Provides execution contexts for RxJava and Kotlin Coroutines.
 * The provided contexts schedule program execution on multiple threads.
 */
@Module
internal object ExecutionContextModule {

    @JvmStatic
    @Provides @Reusable
    fun providesRxSchedulers() = RxSchedulers(
        AndroidSchedulers.mainThread(),
        Schedulers.computation(),
        Schedulers.io(),
        Schedulers.single()
    )

    @JvmStatic
    @Provides @Reusable
    fun providesCoroutineDispatchers() = AppDispatchers(
        Dispatchers.Main,
        Dispatchers.Default,
        Dispatchers.IO
    )
}