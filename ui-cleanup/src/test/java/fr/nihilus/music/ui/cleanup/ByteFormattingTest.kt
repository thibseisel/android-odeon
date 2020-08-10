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

package fr.nihilus.music.ui.cleanup

import io.kotlintest.shouldBe
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class ByteFormattingTest {

    @BeforeTest
    fun setupLocale() {
        Locale.setDefault(Locale.ROOT)
    }

    @Test
    fun `Given negative or zero byte count, when formatting then round to zero`() {
        val negativeByteCount = formatToHumanReadableByteCount(-42)
        negativeByteCount shouldBe "0 o"

        val zeroByteCount = formatToHumanReadableByteCount(0)
        zeroByteCount shouldBe "0 o"
    }

    @Test
    fun `Given less than 1000 bytes, when formatting then display that amount in octets`() {
        val formattedSize = formatToHumanReadableByteCount(956)
        formattedSize shouldBe "956 o"
    }

    @Test
    fun `Given some kilo-octets, when formatting then display as kilo-octet with at most 2 decimals`() {
        val oneKilo = formatToHumanReadableByteCount(1000)
        oneKilo shouldBe "1 ko"

        val someKiloOctets = formatToHumanReadableByteCount(749_583)
        someKiloOctets shouldBe "749.58 ko"

        val roundedKiloOctets = formatToHumanReadableByteCount(46_087)
        roundedKiloOctets shouldBe "46.09 ko"
    }

    @Test
    fun `Given some mega-octets, when formatting then display as mega-octet with at most 2 decimals`() {
        val oneMega = formatToHumanReadableByteCount(1_000_000)
        oneMega shouldBe "1 Mo"

        val someMegaOctets = formatToHumanReadableByteCount(485_172_624)
        someMegaOctets shouldBe "485.17 Mo"

        val roundedMegaOctets = formatToHumanReadableByteCount(91_369_785)
        roundedMegaOctets shouldBe "91.37 Mo"
    }

    @Test
    fun `Given some giga-octets, when formatting then display as giga-octet with at most 2 decimals`() {
        val oneGiga = formatToHumanReadableByteCount(1_000_000_000)
        oneGiga shouldBe "1 Go"

        val someGigaOctets = formatToHumanReadableByteCount(213_820_517_745)
        someGigaOctets shouldBe "213.82 Go"

        val roundedGigaOctet = formatToHumanReadableByteCount(752_128_123_456)
        roundedGigaOctet shouldBe "752.13 Go"
    }
}