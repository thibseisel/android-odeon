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

package fr.nihilus.music.glide.palette

import androidx.annotation.ColorInt
import androidx.palette.graphics.Palette
import fr.nihilus.music.core.ui.extensions.toHsl
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the [PrimaryHueFilter] capacity to reject colors that are too close in hue
 * from the picked primary color.
 * Each test is written using colors manually picked from a real world's album art.
 */
class PrimaryHueFilterTest {

    @Test
    fun blackHolesAndRevelations() {
        // Orange desert ground
        with(PrimaryHueFilter(0xB04020)) {
            assertFalse(primaryIsGreyScale)
            assertTrue(primaryIsNearRed)
            assertNotAllowed(0xB03820) // Near the same orange as primary
            assertAccepted(0x4C61A4) // Blue sky
        }
    }

    @Test
    fun panzerSurprise() {
        // The orange circle background
        with(PrimaryHueFilter(0xD03820)) {
            assertFalse(primaryIsGreyScale)
            assertTrue(primaryIsNearRed)
            assertNotAllowed(0xD03820) // The exact same orange
            assertAccepted(0xA09D34) // The canon's green
            assertAccepted(0xFAB45F) // The circle's yellow
        }
    }

    @Test
    fun nevermind() {
        // Underwater deep blue
        with(PrimaryHueFilter(0x305898)) {
            assertFalse(primaryIsGreyScale)
            assertFalse(primaryIsNearRed)
            assertNotAllowed(0x2F5B9C) // Middle underwater blue
            assertAccepted(0x30A0B0) // Surface light blue
        }
    }

    @Test
    fun garageInc() {
        // James' black jacket
        with(PrimaryHueFilter(0x001820)) {
            assertTrue(primaryIsGreyScale)
            assertFalse(primaryIsNearRed)
            assertAccepted(0x54A5C3) // Kirk's blue face
            assertAccepted(0x80C0E0) // James' blue face
        }
    }

    @Test
    fun blur() {
        // Blurry gold
        with(PrimaryHueFilter(0xF8A020)) {
            assertFalse(primaryIsGreyScale)
            assertFalse(primaryIsNearRed)
            assertNotAllowed(0xFEAE25) // The same gold
            assertNotAllowed(0xFFCE33) // Lighter gold
            assertAccepted(0xF0E080) // Light shades of yellow
        }
    }

    @Test
    fun whitePixelApe() {
        // Light grey wall behind the ape
        with(PrimaryHueFilter(0xF8F8F8)) {
            assertTrue(primaryIsGreyScale)
            assertTrue(primaryIsNearRed)
            assertAccepted(0xE03838) // The ape's straw red lines
        }
    }

    @Test
    fun drones() {
        // Black shadows
        with(PrimaryHueFilter(0x000000)) {
            assertTrue(primaryIsGreyScale)
            assertTrue(primaryIsNearRed)
            assertAccepted(0x833717) // The dark red lever
        }
    }

    private fun Palette.Filter.assertNotAllowed(@ColorInt accentColor: Int) {
        val hsl = accentColor.toHsl()
        assertFalse(isAllowed(accentColor, hsl), "Color should have been rejected. Hue = ${hsl[0]}")
    }

    private fun Palette.Filter.assertAccepted(@ColorInt accentColor: Int) {
        val hsl = accentColor.toHsl()
        assertTrue(isAllowed(accentColor, hsl), "Color should have been accepted. Hue = ${hsl[0]}")
    }
}