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

android {
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
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
}

dependencies {
    implementation(project(":core"))

    // Hilt
    kapt(libs.hilt.compiler)

    // Test dependencies
    testImplementation(project(":core-test"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.androidx.room)
}

tasks.register<Exec>("refreshMediaStore") {
    group = "emulator"
    description = "Scan device's Music folder for files to refresh MediaStore"

    val mediaScannerCommand = """
        "find /storage/emulated/0/Music/ -type f | while read f; do \
        am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE \
        -d \"file://${'$'}{f}\"; done"
    """.trimIndent()
    executable = "adb"
    args("shell", mediaScannerCommand)
}
