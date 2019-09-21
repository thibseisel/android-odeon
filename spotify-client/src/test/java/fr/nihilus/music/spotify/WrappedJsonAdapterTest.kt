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

package fr.nihilus.music.spotify

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import fr.nihilus.music.spotify.model.Artist
import fr.nihilus.music.spotify.model.Image
import fr.nihilus.music.spotify.model.SpotifyError
import fr.nihilus.music.spotify.remote.MULTIPLE_ARTISTS
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import kotlin.test.Test

class WrappedJsonAdapterTest {

    private val moshi = Moshi.Builder().build()

    @Test
    fun `Given wrapped object, when deserializing then return the payload`() {
        val envelopeAdapter = WrappedJsonAdapter(jsonAdapterOf<SpotifyError>(),"error")

        val payload = envelopeAdapter.fromJson("""{
            "error": {
                "status": 404,
                "message": "Not Found"
            }
        }""".trimIndent())

        payload.shouldNotBeNull()
        payload.status shouldBe 404
        payload.message shouldBe "Not Found"
    }

    @org.junit.Test
    fun `Given wrapped array of strings, when deserializing then return the List of strings`() {
        val envelopeAdapter = WrappedJsonAdapter(listAdapterOf<String>(), "data")

        val strings = envelopeAdapter.fromJson("""{
            "data": ["Foo", "Bar"]
        }""".trimIndent())

        strings.shouldContainExactly("Foo", "Bar")
    }

    @Test
    fun `Given wrapped array, when deserializing then return the array as a List`() {
        val envelopeAdapter = WrappedJsonAdapter(listAdapterOf<Artist>(), "artists")

        val artists = envelopeAdapter.fromJson(MULTIPLE_ARTISTS)

        artists.shouldNotBeNull()
        artists shouldHaveSize 2

        with(artists[0]) {
            id shouldBe "12Chz98pHFMPJEknJQMWvI"
            name shouldBe "Muse"
            popularity shouldBe 82
            genres.shouldContainExactly(
                "modern rock",
                "permanent wave",
                "piano rock",
                "post-grunge",
                "rock"
            )
            images.shouldContainExactly(
                Image("https://i.scdn.co/image/17f00ec7613d733f2dd88de8f2c1628ea5f9adde", 320, 320)
            )
        }

        with(artists[1]) {
            id shouldBe "7jy3rLJdDQY21OgRLCZ9sD"
            name shouldBe "Foo Fighters"
            popularity shouldBe 82
            genres.shouldContainExactly(
                "alternative metal",
                "alternative rock",
                "modern rock",
                "permanent wave",
                "post-grunge",
                "rock"
            )
            images.shouldContainExactly(
                Image("https://i.scdn.co/image/c508060cb93f3d2f43ad0dc38602eebcbe39d16d", 320, 320)
            )
        }
    }

    @Test
    fun `Given missing requested property, when deserializing then throw JsonDataException`() {
        val envelopeAdapter = WrappedJsonAdapter(listAdapterOf<String>(), "data")

        shouldThrow<JsonDataException> {
            envelopeAdapter.fromJson("""{
                "payload": ["Foo", "Bar"]
            }""".trimIndent())
        }
    }

    @Test
    fun `Given wrapped null, when deserializing then return null`() {
        val envelopeAdapter = WrappedJsonAdapter(jsonAdapterOf<String>(), "data")

        val payload = envelopeAdapter.fromJson("""{ 
            "data": null
        }""".trimIndent())

        payload.shouldBeNull()
    }

    @Test
    fun `Given null payload, when serializing then wrap null`() {
        val envelopeAdapter = WrappedJsonAdapter(
            jsonAdapterOf<String>(),
            "data"
        ).serializeNulls()

        val json = envelopeAdapter.toJson(null)
        json shouldBe """{"data":null}"""
    }

    @Test
    fun `Given object payload, when serializing then wrap the object`() {
        val envelopeAdapter = WrappedJsonAdapter(jsonAdapterOf<SpotifyError>(), "error")

        val json = envelopeAdapter.toJson(
            SpotifyError(404, "Not Found")
        )

        json shouldBe """{"error":{"status":404,"message":"Not Found"}}"""
    }

    @Test
    fun `Given array payload, when serializing then wrap an array`() {
        val envelopeAdapter = WrappedJsonAdapter(listAdapterOf<String>(), "data")

        val json = envelopeAdapter.toJson(listOf("Foo", "Bar"))
        json shouldBe """{"data":["Foo","Bar"]}"""
    }

    private inline fun <reified T : Any> jsonAdapterOf(): JsonAdapter<T> = moshi.adapter(T::class.java)

    private inline fun <reified E : Any> listAdapterOf(): JsonAdapter<List<E>> {
        val listType = Types.newParameterizedType(List::class.java, E::class.java)
        return moshi.adapter<List<E>>(listType)
    }
}