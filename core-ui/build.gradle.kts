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

android.defaultConfig {
    consumerProguardFiles("consumer-rules.pro")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":media"))

    // Dispatcher to Android main thread
    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Libs.koroutines}")

    api("androidx.media:media:${Libs.Androidx.media}")
    api("androidx.appcompat:appcompat:${Libs.Androidx.appcompat}")
    api("androidx.fragment:fragment-ktx:${Libs.Androidx.fragment}")
    api("androidx.constraintlayout:constraintlayout:${Libs.Androidx.constraint_layout}")
    api("androidx.palette:palette-ktx:${Libs.Androidx.palette}")

    // Android Arch Components
    api("androidx.lifecycle:lifecycle-livedata-ktx:${Libs.Androidx.lifecycle}")
    api("androidx.lifecycle:lifecycle-viewmodel-ktx:${Libs.Androidx.lifecycle}")
    api("androidx.lifecycle:lifecycle-runtime-ktx:${Libs.Androidx.lifecycle}")

    // Navigation Components
    api("androidx.navigation:navigation-fragment-ktx:${Libs.Androidx.navigation}")
    api("androidx.navigation:navigation-ui-ktx:${Libs.Androidx.navigation}")

    // Image loading
    api("com.github.bumptech.glide:glide:${Libs.glide}")
    kapt("com.github.bumptech.glide:compiler:${Libs.glide}")

    // Material Components
    api("com.google.android.material:material:${Libs.material}")

    implementation("com.github.thibseisel:kdenticon-android:${Libs.kdenticon}")

    // Hilt
    kapt("com.google.dagger:hilt-compiler:${Libs.hilt}")

    testImplementation(project(":core-test"))
}
