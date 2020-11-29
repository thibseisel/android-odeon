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

    sourceSets {
        // Add Room schemas to test sources in order to test database migrations.
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
    }
}

dependencies {
    // Shared Kotlin language features
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Libs.koroutines}")

    // Shared AndroidX libraries
    api("androidx.core:core-ktx:${Libs.Androidx.core}")
    api("androidx.work:work-runtime-ktx:${Libs.Androidx.work}")

    // Timber Logging
    api("com.jakewharton.timber:timber:${Libs.timber}")

    // Dagger - compiler is included to generate implementation factories.
    api("com.google.dagger:dagger:${Libs.dagger}")
    kapt("com.google.dagger:dagger-compiler:${Libs.dagger}")

    // Room Database
    api("androidx.room:room-ktx:${Libs.Androidx.room}")
    kapt("androidx.room:room-compiler:${Libs.Androidx.room}")

    // Provides the instance of SharedPreferences
    api("androidx.preference:preference-ktx:${Libs.Androidx.preference}")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:${Libs.kotlin}")
    testImplementation("io.kotest:kotest-assertions-core:${Libs.kotest}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Libs.koroutines}")
    testImplementation("io.mockk:mockk:${Libs.mockk}")
    testImplementation("androidx.test.ext:junit-ktx:${Libs.Androidx.ext_junit}")
    testImplementation ("org.robolectric:robolectric:${Libs.robolectric}") {
        exclude(group = "com.google.auto.service", module = "auto-service")
    }

    androidTestImplementation("org.jetbrains.kotlin:kotlin-test-junit:${Libs.kotlin}")
    androidTestImplementation("androidx.test:core-ktx:${Libs.Androidx.test}")
    androidTestImplementation("androidx.test:runner:${Libs.Androidx.test}")
    androidTestImplementation("androidx.test:rules:${Libs.Androidx.test}")
    androidTestImplementation("androidx.room:room-testing:${Libs.Androidx.room}")
}
