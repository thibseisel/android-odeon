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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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
    fontWeight = FontWeight.Light,
    fontSize = 99.sp,
    letterSpacing = (-0.0151515151).em,
)

private val headline2 = TextStyle(
    fontWeight = FontWeight.Light,
    fontSize = 62.sp,
    letterSpacing = (-0.0080645161).em,
)

private val headline3 = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 49.sp,
    letterSpacing = 0.em,
)

private val headline4 = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 35.sp,
    letterSpacing = 0.0071428571.em,
)

private val headline5 = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 25.sp,
    letterSpacing = 0.em,
)

private val headline6 = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = 21.sp,
    letterSpacing = 0.0071428571.em,
)

private val subtitle1 = TextStyle(
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
    letterSpacing = 0.009375.em,
)

private val subtitle2 = TextStyle(
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    letterSpacing = 0.007142857.em,
)

private val body1 = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    letterSpacing = 0.03125.em,
)

private val body2 = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    letterSpacing = 0.017857142.em,
)

private val button = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 14.sp,
    letterSpacing = 0.089285714.em,
)

private val caption = TextStyle(
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    letterSpacing = 0.0333333333.em,
)

private val overline = TextStyle(
    fontWeight = FontWeight.Medium,
    fontSize = 10.sp,
    letterSpacing = 0.15.em,
)

internal val OdeonTypography = Typography(
    defaultFontFamily = quicksand,
    h1 = headline1,
    h2 = headline2,
    h3 = headline3,
    h4 = headline4,
    h5 = headline5,
    h6 = headline6,
    subtitle1 = subtitle1,
    subtitle2 = subtitle2,
    body1 = body1,
    body2 = body2,
    button = button,
    caption = caption,
    overline = overline
)

@Composable
@Preview
private fun TypeSystem() {
    OdeonTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("Headline1", style = MaterialTheme.typography.h1)
            Text("Headline2", style = MaterialTheme.typography.h2)
            Text("Headline3", style = MaterialTheme.typography.h3)
            Text("Headline4", style = MaterialTheme.typography.h4)
            Text("Headline5", style = MaterialTheme.typography.h5)
            Text("Headline6", style = MaterialTheme.typography.h6)
            Text("Subtitle1", style = MaterialTheme.typography.subtitle1)
            Text("Subtitle2", style = MaterialTheme.typography.subtitle2)
            Text("Body1", style = MaterialTheme.typography.body1)
            Text("Body2", style = MaterialTheme.typography.body2)
            Text("Caption", style = MaterialTheme.typography.caption)
            Text("Button", style = MaterialTheme.typography.button)
            Text("Overline", style = MaterialTheme.typography.overline)
        }
    }
}
