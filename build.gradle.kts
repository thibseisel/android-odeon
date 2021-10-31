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

import com.android.build.gradle.BaseExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.plugin.KaptExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath(libs.plugin.android)
        classpath(libs.plugin.kotlin)
        classpath(libs.plugin.androidx.navigation.safeargs)
        classpath(libs.plugin.hilt)
    }
}

plugins {
    alias(libs.plugins.dependencyupdates)
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter()
    }

    tasks.register("configurations") {
        group = "help"
        description = "Display build configurations declared in project ':${this@allprojects.name}'"

        doLast {
            configurations.filter { it.isCanBeResolved }.forEach {
                println("${it.name} - ${it.description}")
            }
        }
    }
}

subprojects {
    // Common Kotlin configuration
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "11"
            freeCompilerArgs = freeCompilerArgs + arrayOf(
                "-Xopt-in=kotlin.RequiresOptIn",
                "-Xopt-in=kotlin.ExperimentalStdlibApi",
                "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
            )
        }
    }

    tasks.withType<Test>().configureEach {
        testLogging {
            events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
        }
    }

    // Common Android configuration
    afterEvaluate {
        configure<BaseExtension> {
            compileSdkVersion(30)

            defaultConfig {
                minSdk = 21
                targetSdk = 29

                if (testInstrumentationRunner.isNullOrEmpty()) {
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }

            testOptions {
                // Include Android resources in unit tests to be resolved by Robolectric.
                unitTests.isIncludeAndroidResources = true
            }
        }

        if (pluginManager.hasPlugin("org.jetbrains.kotlin.kapt")) {
            configure<KaptExtension> {
                correctErrorTypes = true
                useBuildCache = true
            }
        }

        if (pluginManager.hasPlugin("dagger.hilt.android.plugin")) {
            configure<dagger.hilt.android.plugin.HiltExtension> {
                enableAggregatingTask = true
            }
        }
    }
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
