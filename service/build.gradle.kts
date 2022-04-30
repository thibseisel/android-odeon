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
    id("odeon-convention")
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
}

dependencies {
    implementation(project(":core"))
    api(project(":media"))
    implementation(project(":spotify-client"))

    // Kotlin language support
    implementation(libs.kotlinx.coroutines.android)

    // Android support libraries
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.media)

    // ExoPlayer
    api(libs.exoplayer.core)

    // Dagger
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Glide
    implementation(libs.glide)
    kapt(libs.glide.compiler)

    // Test dependencies
    testImplementation(project(":core-test"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.junit)

    // Android-specific test dependencies
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
}
