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
    id("odeon.android.application")
    id("odeon.android.hilt")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "fr.nihilus.music"
    defaultConfig {
        applicationId = "fr.nihilus.music"
        versionCode = 3_00_00_0
        versionName = "3.0.0"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    // Retrieve keystore properties from this machine's global Gradle properties.
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("release/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }

        create("release") {
            keyAlias = propOrDefault("android.signing.keyAlias", "")
            keyPassword = propOrDefault("android.signing.keyPassword", "")
            storeFile = file(propOrDefault("android.signing.storeFile", "signing/release.jks"))
            storePassword = propOrDefault("android.signing.storePassword", "")
        }
    }

    packaging {
        // Exclude AndroidX version files
        resources.excludes += "META-INF/*.version"
        // Exclude consumer proguard files
        resources.excludes += "META-INF/proguard/*"
        // Exclude the random properties files
        resources.excludes += "/*.properties"
        resources.excludes += "META-INF/*.properties"
    }

    buildTypes {
        // Allow installing a debug version of the application along a production one
        val debug by getting {
            signingConfig = signingConfigs["debug"]
            versionNameSuffix = "-dev"
            applicationIdSuffix = ".debug"
        }

        val release by getting {
            signingConfig = signingConfigs["release"]
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Configure Kotlin compiler optimisations for releases
            kotlinOptions {
                freeCompilerArgs = freeCompilerArgs + listOf(
                    "-Xno-param-assertions",
                    "-Xno-call-assertions",
                    "-Xno-receiver-assertions"
                )
            }
        }

        /* Staging builds are similar to releases but differ by the following:
         * - can be installed alongside the release app,
         * - code is obfuscated, but still debuggable,
         * - APKs are signed with the debug key.
         *
         * The main intent is to test the app in debug mode but with its code obfuscated.
         */
        create("staging") {
            initWith(release)
            matchingFallbacks += release.name
            sourceSets["staging"].setRoot("src/staging")

            versionNameSuffix = "-staging"
            applicationIdSuffix = ".debug"

            // Unlike release builds, keep the app debuggable.
            isDebuggable = true
            signingConfig = debug.signingConfig

            // Keep line number information for obfuscation debugging.
            proguardFiles(
                *release.proguardFiles.toTypedArray(),
                "staging-rules.pro"
            )
        }
    }
}

dependencies {
    // Sub-project containing the Media Service.
    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(projects.media)
    implementation(projects.service)

    implementation(projects.ui.cleanup)
    implementation(projects.ui.settings)
    implementation(projects.ui.library)

    implementation(libs.bundles.core)
    implementation(libs.bundles.android.ui)
    implementation(libs.bundles.androidx.lifecycle)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.viewpager)
    implementation(libs.androidx.media)

    testImplementation(projects.core.testing)
    testImplementation(libs.bundles.testing.unit)

    androidTestImplementation(libs.bundles.testing.instrumented)
}

/**
 * Retrieve a Gradle property value, or return the provided default value if it is not defined.
 *
 * @param propertyName The name of the property to find.
 * @param defaultValue The value to use when the requested property is not defined.
 * @return The value of the Gradle property.
 */
fun <T> Project.propOrDefault(propertyName: String, defaultValue: T): T {
    @Suppress("UNCHECKED_CAST")
    val propertyValue = project.properties[propertyName] as T?
    return propertyValue ?: defaultValue
}
