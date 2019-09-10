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

import dagger.Binds
import dagger.Module
import fr.nihilus.music.media.os.Clock
import fr.nihilus.music.media.os.DeviceClock
import fr.nihilus.music.media.permissions.RuntimePermissions
import fr.nihilus.music.media.permissions.SystemRuntimePermissions

@Module
abstract class CommonModule {

    @Binds
    internal abstract fun bindsSystemPermissions(permissions: SystemRuntimePermissions): RuntimePermissions

    @Binds
    internal abstract fun bindsSystemClock(clock: DeviceClock): Clock
}