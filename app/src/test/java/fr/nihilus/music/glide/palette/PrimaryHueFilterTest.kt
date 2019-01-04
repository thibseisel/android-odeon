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

package fr.nihilus.music.glide.palette

import android.support.annotation.ColorInt
import android.support.v7.graphics.Palette
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import fr.nihilus.music.extensions.toHsl
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests the [PrimaryHueFilter] capacity to reject colors that are too close in hue
 * from the picked primary color.
 * Each test is written using colors manually picked from a real world's album art.
 */
@RunWith(RobolectricTestRunner::class)
class PrimaryHueFilterTest {

    @Test
    fun blackHolesAndRevelations() {
        // Orange desert ground
        with(PrimaryHueFilter(0xB04020)) {
            assertThat(primaryIsGreyScale).isFalse()
            assertThat(primaryIsNearRed).isTrue()
            assertNotAllowed(0xB03820) // Near the same orange as primary
            assertAccepted(0x4C61A4) // Blue sky
        }
    }

    @Test
    fun panzerSurprise() {
        // The orange circle background
        with(PrimaryHueFilter(0xD03820)) {
            assertThat(primaryIsGreyScale).isFalse()
            assertThat(primaryIsNearRed).isTrue()
            assertNotAllowed(0xD03820) // The exact same orange
            assertAccepted(0xA09D34) // The canon's green
            assertAccepted(0xFAB45F) // The circle's yellow
        }
    }

    @Test
    fun nevermind() {
        // Underwater deep blue
        with(PrimaryHueFilter(0x305898)) {
            assertThat(primaryIsGreyScale).isFalse()
            assertThat(primaryIsNearRed).isFalse()
            assertNotAllowed(0x2F5B9C) // Middle underwater blue
            assertAccepted(0x30A0B0) // Surface light blue
        }
    }

    @Test
    fun garageInc() {
        // James' black jacket
        with(PrimaryHueFilter(0x001820)) {
            assertThat(primaryIsGreyScale).isTrue()
            assertThat(primaryIsNearRed).isFalse()
            assertAccepted(0x54A5C3) // Kirk's blue face
            assertAccepted(0x80C0E0) // James' blue face
        }
    }

    @Test
    fun blur() {
        // Blurry gold
        with(PrimaryHueFilter(0xF8A020)) {
            assertThat(primaryIsGreyScale).isFalse()
            assertThat(primaryIsNearRed).isFalse()
            assertNotAllowed(0xFEAE25) // The same gold
            assertNotAllowed(0xFFCE33) // Lighter gold
            assertAccepted(0xF0E080) // Light shades of yellow
        }
    }

    @Test
    fun whitePixelApe() {
        // Light grey wall behind the ape
        with(PrimaryHueFilter(0xF8F8F8)) {
            assertThat(primaryIsGreyScale).isTrue()
            assertThat(primaryIsNearRed).isTrue()
            assertAccepted(0xE03838) // The ape's straw red lines
        }
    }

    @Test
    fun drones() {
        // Black shadows
        with(PrimaryHueFilter(0x000000)) {
            assertThat(primaryIsGreyScale).isTrue()
            assertThat(primaryIsNearRed).isTrue()
            assertAccepted(0x833717) // The dark red lever
        }
    }

    private fun Palette.Filter.assertNotAllowed(@ColorInt accentColor: Int) {
        val hsl = accentColor.toHsl()
        assertWithMessage("Color should have been rejected. Hue = %s", hsl[0])
            .that(isAllowed(accentColor, hsl)).isFalse()
    }

    private fun Palette.Filter.assertAccepted(@ColorInt accentColor: Int) {
        val hsl = accentColor.toHsl()
        assertWithMessage("Color should have been accepted. Hue = %s", hsl[0])
            .that(isAllowed(accentColor, hsl)).isTrue()
    }
}