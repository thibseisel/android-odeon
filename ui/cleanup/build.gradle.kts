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
    id("odeon.android.compose")
}

android {
    namespace = "fr.nihilus.music.ui.cleanup"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(projects.core.compose)
    implementation(projects.media)

    implementation(libs.bundles.core)
    implementation(libs.bundles.android.ui)
    implementation(libs.bundles.androidx.lifecycle)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)

    testImplementation(projects.core.testing)
    testImplementation(libs.bundles.testing.unit)
}
