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

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import fr.nihilus.music.core.ui.R as CoreUIR

private val quicksand = FontFamily(
    Font(CoreUIR.font.quicksand),
    Font(CoreUIR.font.quicksand_light, FontWeight.Light),
    Font(CoreUIR.font.quicksand_semibold, FontWeight.SemiBold),
    Font(CoreUIR.font.quicksand_medium, FontWeight.Medium),
    Font(CoreUIR.font.quicksand_bold, FontWeight.Bold),
)

private val headline1 = TextStyle(
    fontFamily = quicksand,
    fontWeight = FontWeight.Light,
    fontSize = 99.sp,
    letterSpacing = (-0.0151515151).em,
)

private val headline2 = TextStyle(
    fontFamily = quicksand,
    fontWeight = FontWeight.Light,
    fontSize = 62.sp,
    letterSpacing = (-0.0080645161).em,
)

private val headline3 = TextStyle(
    fontFamily = quicksand,
    fontWeight = FontWeight.Normal,
    fontSize = 49.sp,
    letterSpacing = 0.em,
)

private val headline4 = TextStyle(
    fontFamily = quicksand,
    fontWeight = FontWeight.Normal,
    fontSize = 35.sp,
    letterSpacing = 0.0071428571.em,
)

private val headline5 = TextStyle(
    fontFamily = quicksand,
    fontWeight = FontWeight.Normal,
    fontSize = 25.sp,
    letterSpacing = 0.em,
)

private val headline6 = TextStyle(
    fontFamily = quicksand,
    fontWeight = FontWeight.SemiBold,
    fontSize = 21.sp,
    letterSpacing = 0.0071428571.em,
)

private val subtitle1 = TextStyle(
    fontFamily = quicksand,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
    letterSpacing = 0.009375.em,
)

private val subtitle2 = TextStyle(
    fontFamily = quicksand,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    letterSpacing = 0.007142857.em,
)

private val body1 = TextStyle(
    fontFamily = quicksand,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    letterSpacing = 0.03125.em,
)

private val body2 = TextStyle(
    fontFamily = quicksand,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    letterSpacing = 0.017857142.em,
)

private val button = TextStyle(
    fontFamily = quicksand,
    fontWeight = FontWeight.Bold,
    fontSize = 14.sp,
    letterSpacing = 0.089285714.em,
)

private val caption = TextStyle(
    fontFamily = quicksand,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    letterSpacing = 0.0333333333.em,
)

private val overline = TextStyle(
    fontFamily = quicksand,
    fontWeight = FontWeight.Medium,
    fontSize = 10.sp,
    letterSpacing = 0.15.em,
)

internal val OdeonTypography = Typography(
    displayLarge = headline1,
    displayMedium = headline2,
    displaySmall = headline3,
    headlineLarge = headline4,
    headlineMedium = headline4,
    headlineSmall = headline5,
    titleLarge = headline6,
    titleMedium = subtitle1,
    titleSmall = subtitle2,
    bodyLarge = body1,
    bodyMedium = body2,
    bodySmall = caption,
    labelLarge = button,
    labelMedium = button,
    labelSmall = overline,
)
