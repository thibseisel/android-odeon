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
    id("odeon.android.library")
    id("odeon.android.hilt")
}

android {
    namespace = "fr.nihilus.music.core"
    defaultConfig {
        testInstrumentationRunner = "fr.nihilus.music.core.instrumentation.runner.HiltJUnitRunner"
    }
}

dependencies {
    implementation(libs.bundles.core)
    implementation(libs.androidx.lifecycle.process)

    testImplementation(libs.bundles.testing.unit)
    testImplementation(libs.androidx.lifecycle.runtime.testing)

    androidTestImplementation(projects.core.instrumentation)
    androidTestImplementation(libs.bundles.testing.instrumented)
    androidTestImplementation(libs.hilt.android.testing)
    kaptAndroidTest(libs.hilt.compiler)
}
