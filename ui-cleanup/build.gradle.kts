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
    compileSdkVersion(AppConfig.compileSdk)

    defaultConfig {
        minSdkVersion(AppConfig.minSdk)
        targetSdkVersion(AppConfig.targetSdk)

        testInstrumentationRunner("androidx.test.runner.AndroidJUnitRunner")
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":core-ui"))
    implementation(project(":media"))

    implementation("androidx.recyclerview:recyclerview:${Libs.Androidx.recyclerview}")
    implementation("androidx.recyclerview:recyclerview-selection:${Libs.Androidx.rvSelection}")

    kapt("com.google.dagger:dagger-compiler:${Libs.dagger}")
    kapt("com.google.dagger:dagger-android-processor:${Libs.dagger}")

    testImplementation(project(":core-test"))
}