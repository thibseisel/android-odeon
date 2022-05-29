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

android {
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":core-database"))
    implementation(project(":media"))
    implementation(project(":core-ui"))
    implementation(project(":spotify-client"))

    implementation(libs.bundles.core)
    implementation(libs.bundles.android.ui)
    implementation(libs.bundles.androidx.lifecycle)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.work.runtime)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    testImplementation(project(":core-test"))
    testImplementation(libs.bundles.testing.unit)
}
