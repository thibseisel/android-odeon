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
    id("odeon.android.library")
    id("odeon.android.hilt")
}

android {
    namespace = "fr.nihilus.music.spotify"
    defaultConfig {
        consumerProguardFiles("moshi.pro", "okhttp3.pro", "okio.pro")

        val clientId = getSpotifyProperty("clientId")
        val clientSecret = getSpotifyProperty("clientSecret")
        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"$clientId\"")
        buildConfigField("String", "SPOTIFY_CLIENT_SECRET", "\"$clientSecret\"")
    }
}

dependencies {
    implementation(projects.core)
    implementation(projects.coreDatabase)
    implementation(projects.media)

    implementation(libs.bundles.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.json)
    implementation(libs.moshi)
    kapt(libs.moshi.codegen)

    implementation(libs.androidx.work.runtime)

    implementation(libs.androidx.hilt.work)
    kapt(libs.androidx.hilt.compiler)

    testImplementation(projects.coreTest)
    testImplementation(libs.bundles.testing.unit)
    testImplementation(libs.ktor.client.mock)
}

fun getSpotifyProperty(name: String): String {
    val fullPropertyName = "odeon.spotify.$name"
    val value = project.findProperty(fullPropertyName) as? String
    if (value == null) {
        logger.warn("Property \"{}\" is not defined ; sync with Spotify won't work properly.", fullPropertyName)
    }
    return value.orEmpty()
}
