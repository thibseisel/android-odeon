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
        getByName("release") {
            // Configure Kotlin compiler optimisations for releases
            kotlinOptions {
                freeCompilerArgs += listOf(
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

    // Dagger
    kapt("com.google.dagger:dagger-compiler:${Libs.dagger}")

    // Test dependencies
    testImplementation(project(":core-test"))
    testImplementation("org.robolectric:robolectric:${Libs.robolectric}")
    testImplementation("androidx.test.ext:junit-ktx:${Libs.Androidx.ext_junit}")
    testImplementation("androidx.room:room-ktx:${Libs.Androidx.room}")
    kaptTest("com.google.dagger:dagger-compiler:${Libs.dagger}")
}
