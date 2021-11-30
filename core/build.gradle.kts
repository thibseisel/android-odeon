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
        testInstrumentationRunner = "fr.nihilus.music.core.instrumentation.runner.HiltJUnitRunner"
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
    api(libs.kotlinx.coroutines.core)

    // Shared AndroidX libraries
    api(libs.androidx.core)
    api(libs.androidx.work.runtime)

    // Timber Logging
    api(libs.timber)

    // Hilt
    api(libs.hilt.android)
    implementation(libs.androidx.hilt.work)
    kapt(libs.hilt.compiler)

    // Room Database
    api(libs.androidx.room)
    kapt(libs.androidx.room.compiler)

    // Provides the instance of SharedPreferences
    api(libs.androidx.preference)

    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.test.junit)
    testImplementation (libs.robolectric) {
        exclude(group = "com.google.auto.service", module = "auto-service")
    }

    androidTestImplementation(project(":core-instrumentation"))
    androidTestImplementation(libs.kotlin.test.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotest.assertions.core)
    androidTestImplementation(libs.hilt.android.testing)
    kaptAndroidTest(libs.hilt.compiler)

    constraints {
        // Force androidx.room compiler to use a SQLite version compatible with Apple Silicon
        kapt("org.xerial:sqlite-jdbc") {
            version {
                // Apple Silicon support has been introduced in this version
                require("3.32.3.3")
                // Version below failed to include M1 binary
                reject("3.35.0")
            }
        }
    }
}
