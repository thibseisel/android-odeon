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
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    defaultConfig {
        applicationId("fr.nihilus.music")
        versionCode(2_01_00_5)
        versionName("2.1.0")
    }

    buildFeatures {
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

    packagingOptions {
        // Exclude AndroidX version files
        exclude("META-INF/*.version")
        // Exclude consumer proguard files
        exclude("META-INF/proguard/*")
        // Exclude the random properties files
        exclude("/*.properties")
        exclude("META-INF/*.properties")

        // Fix weird packaging bugs due to including Ktor
        pickFirst("META-INF/*.kotlin_module")
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
            setMatchingFallbacks(release.name)
            sourceSets["staging"].setRoot("src/staging")

            versionNameSuffix = "-staging"
            applicationIdSuffix = ".debug"

            // Unlike release builds, keep the app debuggable.
            debuggable(true)
            setSigningConfig(debug.signingConfig)

            // Keep line number information for obfuscation debugging.
            proguardFiles(
                *release.proguardFiles.toTypedArray(),
                "staging-rules.pro"
            )
        }
    }

    kapt {
        arguments {
            // Configure Dagger code generation
            arg("dagger.formatGeneratedSource", "disabled")
            arg("dagger.fastInit", "enabled")
        }
    }
}

dependencies {
    // Sub-project containing the Media Service.
    implementation(project(":core"))
    implementation(project(":core-ui"))
    implementation(project(":media"))
    implementation(project(":service"))
    implementation(project(":spotify-client"))

    implementation(project(":ui-cleanup"))
    implementation(project(":ui-settings"))

    // Support library dependencies
    implementation("androidx.recyclerview:recyclerview:${Libs.Androidx.recyclerview}")
    implementation("androidx.viewpager2:viewpager2:${Libs.Androidx.viewpager2}")

    // Dagger
    kapt("com.google.dagger:dagger-compiler:${Libs.dagger}")
    kapt("com.google.dagger:dagger-android-processor:${Libs.dagger}")

    // Test dependencies
    testImplementation(project(":core-test"))
    testImplementation("androidx.test:rules:${Libs.Androidx.test}")
    testImplementation("androidx.test.ext:junit-ktx:${Libs.Androidx.ext_junit}")
    testImplementation("org.robolectric:robolectric:${Libs.robolectric}")

    androidTestImplementation("androidx.test:core:${Libs.Androidx.test}")
    androidTestImplementation("androidx.test:rules:${Libs.Androidx.test}")
    androidTestImplementation("androidx.test:runner:${Libs.Androidx.test}")
    androidTestImplementation("androidx.test.espresso:espresso-core:${Libs.Androidx.espresso}")
}
