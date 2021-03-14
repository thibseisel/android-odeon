/*
 * Copyright 2020 Thibault Seisel
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

package fr.nihilus.music.dagger

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.nihilus.music.core.ui.dagger.CoreUiModule
import fr.nihilus.music.core.ui.viewmodel.ViewModelModule
import fr.nihilus.music.library.HomeModule
import fr.nihilus.music.ui.cleanup.CleanupModule
import fr.nihilus.music.ui.settings.SettingsModule

/**
 * Configure dependencies for the client-side GUI application.
 * This groups all dependencies required to display and interaction with the media browser service.
 */
@Module(includes = [
    HomeModule::class,
    CleanupModule::class,
    SettingsModule::class,
    ViewModelModule::class,
    CoreUiModule::class
])
@InstallIn(SingletonComponent::class)
internal abstract class ClientModule