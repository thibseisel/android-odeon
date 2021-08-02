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
    id("dagger.hilt.android.plugin")
}

android {
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("release") {
            // Configure Kotlin compiler optimisations for releases
            kotlinOptions {
                freeCompilerArgs = freeCompilerArgs + listOf(
                    "-Xno-param-assertions",
                    "-Xno-call-assertions",
                    "-Xno-receiver-assertions"
                )
            }
        }
    }
}

dependencies {
    implementation(project(":core"))
    api(project(":media"))
    implementation(project(":spotify-client"))

    // Kotlin language support
    implementation(KotlinX.coroutines.android)

    // Android support libraries
    implementation(AndroidX.appCompat)
    implementation(AndroidX.media)

    // ExoPlayer
    api("com.google.android.exoplayer:exoplayer-core:_")

    // Dagger
    implementation(Google.dagger.hilt.android)
    kapt(Google.dagger.hilt.compiler)

    // Glide
    implementation("com.github.bumptech.glide:glide:_")
    kapt("com.github.bumptech.glide:compiler:_")

    // Test dependencies
    testImplementation(project(":core-test"))
    testImplementation(Testing.robolectric)
    testImplementation(AndroidX.test.ext.junitKtx)

    // Android-specific test dependencies
    androidTestImplementation(AndroidX.test.core)
    androidTestImplementation(AndroidX.test.runner)
    androidTestImplementation(AndroidX.test.ext.junitKtx)
}
