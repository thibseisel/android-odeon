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

package fr.nihilus.music.media.actions

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import fr.nihilus.music.common.media.CustomActions

@Module
abstract class CustomActionModule {

    @Binds @IntoMap
    @StringKey(CustomActions.ACTION_DELETE_MEDIA)
    internal abstract fun bindsDeleteAction(action: DeleteAction): BrowserAction

    @Binds @IntoMap
    @StringKey(CustomActions.ACTION_MANAGE_PLAYLIST)
    internal abstract fun bindsManagePlaylistAction(action: ManagePlaylistAction): BrowserAction
}