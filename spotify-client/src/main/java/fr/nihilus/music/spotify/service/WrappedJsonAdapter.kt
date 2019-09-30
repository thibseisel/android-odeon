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

package fr.nihilus.music.spotify.service

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

/**
 * A custom JSON serializer/deserializer that wraps/unwraps payloads of type [T]
 * in a JSON object associated with the specified [property].
 *
 * While this adapter is initially meant to deserialize JSON arrays
 * that have been protected against JSON Hijacking,
 * a known security vulnerability of JavaScript combined with a Cross-Site Request Forgery
 * allowing attackers to read sensitive data from JSON received in the browser,
 * it can also deserialize JSON objects.
 *
 * For example, given the following JSON:
 * ```json
 * {
 *   "payload": {
 *     "first_name": "John",
 *     "last_name": "Doe"
 *   }
 * }
 * ```
 * When deserializing to an object of type `Person` with an adapter created like the following:
 * ```kotlin
 * WrappedJsonAdapter("payload", moshi.adapter(Person::class.java)
 * ```
 * the deserializer will ignore the wrapping object and start reading at its `payload` property.
 *
 * @param delegate The adapter to use when serializing/deserializing a payload of type [T].
 * @param property The name of the property the payload is associated to.
 */
class WrappedJsonAdapter<T>(
    private val property: String,
    private val delegate: JsonAdapter<T>
) : JsonAdapter<T>() {

    private val options = JsonReader.Options.of(property)

    override fun fromJson(reader: JsonReader): T? {
        var payload: T? = null
        var hasFoundEnvelope = false

        reader.beginObject()
        while (reader.hasNext()) {
            if (reader.selectName(options) == 0) {
                payload = delegate.fromJson(reader)
                hasFoundEnvelope = true
            } else {
                reader.skipName()
                reader.skipValue()
            }
        }

        reader.endObject()

        if (!hasFoundEnvelope) throw JsonDataException("Missing envelope property '$property'.")
        return payload
    }

    override fun toJson(writer: JsonWriter, value: T?) {
        writer.beginObject()
        writer.name(property)
        if (value == null) {
            writer.nullValue()
        } else {
            delegate.toJson(writer, value)
        }
        writer.endObject()
    }
}