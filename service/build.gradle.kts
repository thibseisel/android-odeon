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

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
}

android {
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("String", "APP_PROVIDER_AUTHORITY", "\"fr.nihilus.music.debug.provider\"")
            manifestPlaceholders["providerAuthority"] = "fr.nihilus.music.debug.provider"
        }

        getByName("release") {
            // Configure Kotlin compiler optimisations for releases
            kotlinOptions {
                freeCompilerArgs += listOf(
                    "-Xno-param-assertions",
                    "-Xno-call-assertions",
                    "-Xno-receiver-assertions"
                )
            }

            buildConfigField("String", "APP_PROVIDER_AUTHORITY", "\"fr.nihilus.music.provider\"")
            manifestPlaceholders["providerAuthority"] = "fr.nihilus.music.provider"
        }
    }
}

dependencies {
    implementation(project(":core"))
    api(project(":media"))
    implementation(project(":spotify-client"))

    // Kotlin language support
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Libs.koroutines}")

    // Android support libraries
    implementation("androidx.appcompat:appcompat:${Libs.Androidx.appcompat}")
    implementation("androidx.media:media:${Libs.Androidx.media}")

    // ExoPlayer
    api("com.google.android.exoplayer:exoplayer-core:${Libs.exoplayer}")

    // Dagger
    implementation("com.google.dagger:dagger-android:${Libs.dagger}")
    kapt("com.google.dagger:dagger-compiler:${Libs.dagger}")
    kapt("com.google.dagger:dagger-android-processor:${Libs.dagger}")

    // Glide
    implementation("com.github.bumptech.glide:glide:${Libs.glide}")
    kapt("com.github.bumptech.glide:compiler:${Libs.glide}")

    // Test dependencies
    testImplementation(project(":core-test"))
    testImplementation("org.robolectric:robolectric:${Libs.robolectric}")
    testImplementation("androidx.test.ext:junit-ktx:${Libs.Androidx.ext_junit}")

    // Android-specific test dependencies
    androidTestImplementation("androidx.test:core:${Libs.Androidx.test}")
    androidTestImplementation("androidx.test:runner:${Libs.Androidx.test}")
    androidTestImplementation("androidx.test.ext:junit-ktx:${Libs.Androidx.ext_junit}")
}
