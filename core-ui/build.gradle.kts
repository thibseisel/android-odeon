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

android.defaultConfig {
    consumerProguardFiles("consumer-rules.pro")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":media"))

    // Dispatcher to Android main thread
    api(KotlinX.coroutines.android)

    api(AndroidX.media)
    api(AndroidX.appCompat)
    api(AndroidX.fragmentKtx)
    api(AndroidX.constraintLayout)
    api(AndroidX.paletteKtx)

    // Android Arch Components
    api(AndroidX.lifecycle.liveDataKtx)
    api(AndroidX.lifecycle.viewModelKtx)
    api(AndroidX.lifecycle.runtimeKtx)

    // Navigation Components
    api(AndroidX.navigation.fragmentKtx)
    api(AndroidX.navigation.uiKtx)

    // Image loading
    api("com.github.bumptech.glide:glide:_")
    kapt("com.github.bumptech.glide:compiler:_")

    // Material Components
    api(Google.android.material)

    implementation("com.github.thibseisel:kdenticon-android:_")

    // Hilt
    kapt(Google.dagger.hilt.compiler)

    testImplementation(project(":core-test"))
}
