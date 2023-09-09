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
    id("odeon.android.library")
    id("odeon.android.hilt")
    alias(libs.plugins.ksp)
}

val schemaDir = File(projectDir, "schemas")

android {
    namespace = "fr.nihilus.music.core.database"
    sourceSets {
        // Add Room schemas to test sources in order to test database migrations.
        getByName("androidTest").assets.srcDir(schemaDir)
    }
}

ksp {
    arg(RoomSchemaArgProvider(schemaDir))
}

dependencies {
    implementation(libs.bundles.core)
    implementation(libs.androidx.room)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.bundles.testing.unit)

    androidTestImplementation(projects.coreInstrumentation)
    androidTestImplementation(libs.bundles.testing.instrumented)
    androidTestImplementation(libs.androidx.room.testing)

    constraints {
        ksp("org.xerial:sqlite-jdbc") {
            because("Apple Silicon support has been introduced in 3.32.3.3")
            version {
                require("3.32.3.3")
                // Version below is known to have failed to include M1 binary
                reject("3.35.0")
            }
        }
    }
}

/**
 * Specifies the directory in which Room schema definition files are stored.
 *
 * This syntax workarounds a bug in AndroidX Room regarding incremental Gradle processing.
 * Once it is fixed, this could be replaced by the simpler
 * `arg("room.schemaLocation", "path/to/file")` syntax.
 */
private class RoomSchemaArgProvider(
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val schemaDir: File
) : CommandLineArgumentProvider {

    override fun asArguments(): Iterable<String> = listOf(
        "room.schemaLocation=${schemaDir.path}"
    )
}
