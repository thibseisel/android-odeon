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

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

private val materialPurple300 = Color(0xFFB39DDB)
private val materialPurple500 = Color(0xFF673AB7)
private val materialPurple700 = Color(0xFF512DA8)
private val materialOrangeA100 = Color(0xFFFFD180)
private val materialOrangeA200 = Color(0xFFFFAB40)

internal val OdeonLightColors = lightColors(
    primary = materialPurple500,
    primaryVariant = materialPurple700,
    secondary = materialOrangeA200,
    secondaryVariant = materialOrangeA200,
    onPrimary = Color.White,
    onSecondary = Color.Black,
)

internal val OdeonDarkColors = darkColors(
    primary = materialPurple300,
    primaryVariant = materialPurple500,
    secondary = materialOrangeA100,
    secondaryVariant = materialOrangeA100,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
)
