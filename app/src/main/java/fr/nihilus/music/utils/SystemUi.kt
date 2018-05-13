/*
 * Copyright 2018 Thibault Seisel
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

package fr.nihilus.music.utils

import android.os.Build
import android.support.annotation.RequiresApi
import android.view.View
import android.view.Window

/**
 * Changes the text and icons color of the status bar for a given [window].
 *
 * @param window The window for which the color should be changed.
 * @param dark `true` for dark text and icons, `false` for white ones.
 */
@RequiresApi(Build.VERSION_CODES.M)
fun setLightStatusBar(window: Window, dark: Boolean) = with(window.decorView) {
    systemUiVisibility = if (dark) {
        systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    } else {
        systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
    }
}