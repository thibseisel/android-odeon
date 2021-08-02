/*
 * Copyright 2021 Thibault Seisel
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

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
}

android {
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro", "moshi.pro", "okhttp3.pro", "okio.pro")

        val clientId = getSpotifyProperty("clientId")
        val clientSecret = getSpotifyProperty("clientSecret")
        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"$clientId\"")
        buildConfigField("String", "SPOTIFY_CLIENT_SECRET", "\"$clientSecret\"")
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":media"))

    // Ktor Client - MultiPlatform HTTP Client
    api(Ktor.client.okHttp)
    implementation(Ktor.client.json)

    // Moshi - Kotlin JSON serialization
    api(Square.moshi)
    kapt(Square.moshi.kotlinCodegen)

    implementation(Google.dagger.hilt.android)
    implementation(AndroidX.hilt.work)
    kapt(Google.dagger.hilt.compiler)
    kapt(AndroidX.hilt.compiler)

    testImplementation(project(":core-test"))
    testImplementation("io.ktor:ktor-client-mock-jvm:_")
    testImplementation(Testing.mockK)
}

fun getSpotifyProperty(name: String): String {
    val fullPropertyName = "odeon.spotify.$name"
    val value = project.findProperty(fullPropertyName) as? String
    if (value == null) {
        logger.warn("Property \"{}\" is not defined ; sync with Spotify won't work properly.", fullPropertyName)
    }
    return value.orEmpty()
}
