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

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
}

android {
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro", "moshi.pro", "okhttp3.pro", "okio.pro")

        val clientId = propOrDefault("odeon.spotify.clientId", "")
        val clientSecret = propOrDefault("odeon.spotify.clientSecret", "")
        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"$clientId\"")
        buildConfigField("String", "SPOTIFY_CLIENT_SECRET", "\"$clientSecret\"")
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":media"))

    // Ktor Client - MultiPlatform HTTP Client
    api("io.ktor:ktor-client-okhttp:${Libs.ktor}")
    implementation("io.ktor:ktor-client-json:${Libs.ktor}")

    // Moshi - Kotlin JSON serialization
    api("com.squareup.moshi:moshi:${Libs.moshi}")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:${Libs.moshi}")
    kapt("com.google.dagger:dagger-compiler:${Libs.dagger}")

    testImplementation(project(":core-test"))
    testImplementation("io.ktor:ktor-client-mock-jvm:${Libs.ktor}")
    testImplementation("io.mockk:mockk:${Libs.mockk}")
    kaptTest("com.google.dagger:dagger-compiler:${Libs.dagger}")
}
