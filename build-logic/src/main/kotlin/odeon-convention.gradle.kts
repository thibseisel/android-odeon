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

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.BaseExtension
import dagger.hilt.android.plugin.HiltExtension
import odeon.tasks.RefreshMediaStore
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.plugin.KaptExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

pluginManager.withPlugin("com.android.application") {
    configureAndroid(isLibrary = false)
    configureApplicationTasks()
}

pluginManager.withPlugin("com.android.library") {
    configureAndroid(isLibrary = true)
}

pluginManager.withPlugin("org.jetbrains.kotlin.android") {
    configureKotlin()
}

pluginManager.withPlugin("org.jetbrains.kotlin.kapt") {
    configure<KaptExtension> {
        correctErrorTypes = true
        useBuildCache = true
    }
}

pluginManager.withPlugin("dagger.hilt.android.plugin") {
    configure<HiltExtension> {
        enableAggregatingTask = true
    }
    dependencies {
        addProvider("implementation", libs.findLibrary("hilt-android").get())
        addProvider("kapt", libs.findLibrary("hilt-compiler").get())
    }
}

fun configureAndroid(isLibrary: Boolean) {
    apply(plugin = "org.gradle.android.cache-fix")

    configure<BaseExtension> {
        compileSdkVersion(31)

        defaultConfig {
            minSdk = 23
            targetSdk = 31
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            vectorDrawables.useSupportLibrary = true

            if (isLibrary && file("consumer-rules.pro").exists()) {
                consumerProguardFile("consumer-rules.pro")
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
}

fun configureKotlin() {
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "11"
            freeCompilerArgs = freeCompilerArgs + arrayOf(
                "-progressive",
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
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
}

fun configureApplicationTasks() {
    val androidComponents = extensions.getByType(AndroidComponentsExtension::class)
    tasks.register<RefreshMediaStore>("refreshMediaStore") {
        group = "emulator"
        description = "Scan the connected device's Music folder for files to refresh MediaStore."
        @Suppress("UnstableApiUsage")
        adbPath.set(androidComponents.sdkComponents.adb)
    }
}
