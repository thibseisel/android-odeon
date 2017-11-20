/*
 * Copyright 2017 Thibault Seisel
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

package fr.nihilus.music.media.builtin

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet

/**
 * A Dagger module that binds built-in items of the music library into a set.
 *
 * All items placed into the set should be displayed together as part of the main screen of the UI.
 */
@Suppress("unused")
@Module
internal abstract class HomeScreenModule {

    @Binds @IntoSet
    abstract fun bindMostRecents(playlist: MostRecentTracks): BuiltinItem
}