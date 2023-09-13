/*
 * Copyright 2023 Thibault Seisel
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

package fr.nihilus.music.core.compose.theme

import androidx.compose.material.Colors
import androidx.compose.ui.graphics.Color

/**
 * Color to be applied as the background of a enabled selectable control.
 */
val Colors.selectableBackground: Color
    get() = if (isLight) {
        primary.copy(alpha = 0.08f)
    } else {
        onSurface.copy(alpha = 0.16f)
    }