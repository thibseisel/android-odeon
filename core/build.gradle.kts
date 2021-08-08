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
    api(KotlinX.coroutines.core)

    // Shared AndroidX libraries
    api(AndroidX.core.ktx)
    api(AndroidX.work.runtimeKtx)

    // Timber Logging
    api(JakeWharton.timber)

    // Hilt
    api(Google.dagger.hilt.android)
    implementation(AndroidX.hilt.work)
    kapt(Google.dagger.hilt.compiler)

    // Room Database
    api(AndroidX.room.ktx)
    kapt(AndroidX.room.compiler)

    // Provides the instance of SharedPreferences
    api(AndroidX.preferenceKtx)

    testImplementation(Kotlin.test.junit)
    testImplementation(Testing.kotest.assertions.core)
    testImplementation(KotlinX.coroutines.test)
    testImplementation(Testing.mockK)
    testImplementation(AndroidX.test.ext.junitKtx)
    testImplementation (Testing.robolectric) {
        exclude(group = "com.google.auto.service", module = "auto-service")
    }

    androidTestImplementation(project(":core-instrumentation"))
    androidTestImplementation(Kotlin.test.junit)
    androidTestImplementation(AndroidX.test.coreKtx)
    androidTestImplementation(AndroidX.test.runner)
    androidTestImplementation(AndroidX.test.rules)
    androidTestImplementation(AndroidX.room.testing)
    androidTestImplementation(Testing.kotest.assertions.core)
    androidTestImplementation(Google.dagger.hilt.android.testing)
    kaptAndroidTest(Google.dagger.hilt.compiler)
}
