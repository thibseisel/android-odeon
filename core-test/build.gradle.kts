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
}

dependencies {
    api(project(":core"))

    api("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Libs.koroutines}")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-debug:${Libs.koroutines}")
    api("androidx.test:core:${Libs.Androidx.test}")
    api("androidx.test:runner:${Libs.Androidx.test}")
    api("androidx.test.ext:junit-ktx:${Libs.Androidx.ext_junit}")
    api("org.jetbrains.kotlin:kotlin-test-junit:${Libs.kotlin}")
    api("io.kotest:kotest-assertions-core:${Libs.kotest}")
    api("io.kotest:kotest-property:${Libs.kotest}")
    api("io.mockk:mockk:${Libs.mockk}")
    api ("org.robolectric:robolectric:${Libs.robolectric}") {
        exclude(group = "com.google.auto.service", module = "auto-service")
    }
}
