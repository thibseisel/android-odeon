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

package fr.nihilus.music.ui.cleanup

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class FileSizeTest {
    @BeforeTest
    fun setupLocale() {
        Locale.setDefault(Locale.ROOT)
    }

    @Test
    fun `throws given a negative byte count`() {
        shouldThrowWithMessage<IllegalArgumentException>("Invalid file size: -42 bytes") {
            FileSize(-42)
        }
    }

    @Test
    fun `rounds to zero given a negative or zero byte count`() {
        FileSize(0).toString() shouldBe "0 o"
    }

    @Test
    fun `formats byte count as-is given less than 1000 bytes`() {
        FileSize(956).toString() shouldBe "956 o"
    }

    @Test
    fun `formats with at most 2 decimals given kilo-octets`() {
        FileSize(1000).toString() shouldBe "1 ko"
        FileSize(46_087).toString() shouldBe "46.09 ko"
        FileSize(749_583).toString() shouldBe "749.58 ko"
    }

    @Test
    fun `formats with at most 2 decimals given mega-octets`() {
        FileSize(1_000_000).toString() shouldBe "1 Mo"
        FileSize(485_172_624).toString() shouldBe "485.17 Mo"
        FileSize(91_369_785).toString() shouldBe "91.37 Mo"
    }

    @Test
    fun `formats with at most 2 decimals given giga-octets`() {
        FileSize(1_000_000_000).toString() shouldBe "1 Go"
        FileSize(213_820_517_745).toString() shouldBe "213.82 Go"
        FileSize(752_128_123_456).toString() shouldBe "752.13 Go"
    }
}
