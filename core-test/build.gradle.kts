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

    api(libs.kotlinx.coroutines.test)
    api(libs.kotlinx.coroutines.debug)
    api(libs.androidx.test.core)
    api(libs.androidx.test.runner)
    api(libs.androidx.test.junit)
    api(libs.kotlin.test.junit)
    api(libs.kotest.assertions.core)
    api(libs.kotest.property)
    api(libs.mockk)
    api(libs.turbine)
    api(libs.robolectric)
}
