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

package fr.nihilus.music.common

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import fr.nihilus.music.common.context.AppDispatchers
import fr.nihilus.music.common.context.ExecutionContextModule
import fr.nihilus.music.common.context.RxSchedulers
import fr.nihilus.music.common.database.SQLiteDatabaseModule
import fr.nihilus.music.common.database.playlists.PlaylistDao
import fr.nihilus.music.common.database.spotify.SpotifyDao
import fr.nihilus.music.common.database.usage.UsageDao
import fr.nihilus.music.common.os.Clock
import fr.nihilus.music.common.os.RuntimePermissions
import fr.nihilus.music.common.settings.Settings
import javax.inject.Singleton

@Singleton
@Component(modules = [
    CommonModule::class,
    ExecutionContextModule::class,
    SQLiteDatabaseModule::class
])
interface CoreComponent {
    //val appScope: CoroutineScope
    val dispatchers: AppDispatchers
    val schedulers: RxSchedulers
    val permissions: RuntimePermissions
    val clock: Clock
    val playlistDao: PlaylistDao
    val usageDao: UsageDao
    val spotifyDao: SpotifyDao
    val settings: Settings

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance applicationContext: Context): CoreComponent
    }
}