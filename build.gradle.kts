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

import com.android.build.gradle.BaseExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        jcenter()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:4.1.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Libs.kotlin}")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:${Libs.Androidx.navigation}")
    }
}

plugins {
    id("com.github.ben-manes.versions") version "0.33.0"
}

allprojects {
    repositories {
        google()
        jcenter()
    }

    tasks.register("configurations") {
        group = "help"
        description = "Display build configurations declared in project ':${this@allprojects.name}'"

        doLast {
            for (it in configurations) {
                println("${it.name} - ${it.description}")
            }
        }
    }
}

subprojects {
    // Common Kotlin configuration
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs += arrayOf(
                "-Xopt-in=kotlin.RequiresOptIn",
                "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
            )
        }
    }

    // Common Android configuration
    afterEvaluate {
        configure<BaseExtension> {
            compileSdkVersion(AppConfig.compileSdk)

            defaultConfig {
                minSdkVersion(AppConfig.minSdk)
                targetSdkVersion(AppConfig.targetSdk)

                testInstrumentationRunner("androidx.test.runner.AndroidJUnitRunner")
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_1_8
                targetCompatibility = JavaVersion.VERSION_1_8
            }

            testOptions {
                // Include Android resources in unit tests to be resolved by Robolectric.
                unitTests.isIncludeAndroidResources = true
            }
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}

tasks.withType<Wrapper> {
    distributionType = Wrapper.DistributionType.ALL
}