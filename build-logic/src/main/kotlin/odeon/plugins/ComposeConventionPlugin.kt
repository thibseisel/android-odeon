/*
 * Copyright 2023 Thibault Seisel
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

package odeon.plugins

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType

/**
 * Configures an Android library project to use AndroidX Compose.
 */
@Suppress("unused")
internal class ComposeConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        configureAndroidCompose()
        configureComposeDependencies()
    }

    private fun Project.configureComposeDependencies() {
        val catalog = getVersionCatalog()
        val composeBom = catalog.findLibrary("compose-bom").get()

        dependencies {
            add("implementation", platform(composeBom))
            add("implementation", catalog.findLibrary("compose-ui-tooling-preview").get())
            add("debugImplementation", catalog.findLibrary("compose-ui-tooling").get())
            add("debugImplementation", catalog.findLibrary("compose-ui-test-manifest").get())
            add("androidTestImplementation", platform(composeBom))
        }
    }

    private fun Project.configureAndroidCompose() {
        val android = extensions.findByType<LibraryExtension>()
        checkNotNull(android) {
            "Plugin \"odeon.compose\" also requires applying \"odeon.library\""
        }

        android.apply {
            buildFeatures.compose = true

            composeOptions {
                kotlinCompilerExtensionVersion = resolveComposeCompilerVersion()
            }
        }
    }

    private fun Project.resolveComposeCompilerVersion(): String {
        val libs = getVersionCatalog()
        return libs
            .findVersion("compose-compiler")
            .orElseThrow { IllegalStateException("Expected a library version named \"compose-compiler\"") }
            .toString()
    }


    private fun Project.getVersionCatalog(): VersionCatalog {
        return extensions.getByType<VersionCatalogsExtension>()
            .find("libs")
            .orElseThrow { IllegalStateException("Expected a version catalog named \"libs\"") }
    }
}