/*
 * Copyright 2022 Thibault Seisel
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
    implementation(libs.bundles.core)
    implementation(libs.androidx.room)
    kapt(libs.androidx.room.compiler)

    testImplementation(libs.bundles.testing.unit)

    androidTestImplementation(project(":core-instrumentation"))
    androidTestImplementation(libs.bundles.testing.instrumented)
    androidTestImplementation(libs.androidx.room.testing)

    constraints {
        kapt("org.xerial:sqlite-jdbc") {
            because("Apple Silicon support has been introduced in 3.32.3.3")
            version {
                require("3.32.3.3")
                // Version below is known to have failed to include M1 binary
                reject("3.35.0")
            }
        }
    }
}
