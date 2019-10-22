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

package fr.nihilus.music.core.ui.dagger

import dagger.Binds
import dagger.Module
import fr.nihilus.music.core.ui.client.BrowserClient
import fr.nihilus.music.core.ui.client.BrowserClientImpl

/**
 * Provides an implementation of [BrowserClient] that connects to MusicService
 * to read media hierarchy and send playback commands.
 */
@Module
abstract class CoreUiModule {

    @Binds
    internal abstract fun bindsBrowserClient(impl: BrowserClientImpl): BrowserClient
}