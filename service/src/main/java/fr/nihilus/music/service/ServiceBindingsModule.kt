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

package fr.nihilus.music.service

import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.scopes.ServiceScoped
import fr.nihilus.music.service.browser.BrowserTree
import fr.nihilus.music.service.browser.BrowserTreeImpl
import fr.nihilus.music.service.metadata.GlideDownloader
import fr.nihilus.music.service.metadata.IconDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier

@Module
@InstallIn(ServiceComponent::class)
internal abstract class ServiceBindingsModule {

    @Binds
    abstract fun bindsBrowserTree(impl: BrowserTreeImpl): BrowserTree

    @Binds
    abstract fun bindsSubscriptionManager(impl: CachingSubscriptionManager): SubscriptionManager

    @Binds
    abstract fun bindsPlayer(player: ExoPlayer): Player

    @Binds
    abstract fun bindsIconDownloader(downloader: GlideDownloader): IconDownloader

    companion object {
        @Provides @ServiceScoped
        @ServiceCoroutineScope
        fun providesServiceScope() = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    }
}

@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class ServiceCoroutineScope