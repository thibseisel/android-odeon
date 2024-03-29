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

buildscript {
    dependencies {
        classpath(libs.plugin.hilt)
    }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.versions)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.androidx.navigation.safeargs.kotlin) apply false
}

tasks.register<Exec>("startAutoDhu") {
    group = "android-auto"
    description = "Start Android Auto's Desktop Head Unit (DHU)"

    val androidSdkRoot = System.getenv("ANDROID_SDK_ROOT")
        ?: "${System.getenv("user.home")}/android-sdk"

    workingDir("$androidSdkRoot/extras/google/auto")
    standardInput = System.`in`

    if (System.getProperty("os.name").contains("windows", true)) {
        executable = "cmd"
        args("/c", "desktop-head-unit")
    } else {
        executable = "desktop-head-unit"
    }

    doFirst {
        exec {
            workingDir("$androidSdkRoot/platform-tools")
            executable = "adb"
            args("forward", "tcp:5277", "tcp:5277")
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}

tasks.withType<Wrapper>().configureEach {
    distributionType = Wrapper.DistributionType.BIN
}

tasks.dependencyUpdates.configure {
    revision = "release"
    gradleReleaseChannel = "current"

    val releaseRegex = Regex("^[0-9,.v-]+(-r)?\$", RegexOption.IGNORE_CASE)
    rejectVersionIf { !candidate.version.matches(releaseRegex) }
}
