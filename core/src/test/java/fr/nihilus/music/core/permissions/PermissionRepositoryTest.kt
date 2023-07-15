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

package fr.nihilus.music.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
internal class PermissionRepositoryTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeLifecycleOwner = TestLifecycleOwner(
        initialState = Lifecycle.State.INITIALIZED,
        coroutineDispatcher = testDispatcher,
    )

    @MockK private lateinit var mockContext: Context
    private lateinit var repository: PermissionRepository

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        MockKAnnotations.init(this, relaxUnitFun = true)
        setFakePermission(Manifest.permission.READ_EXTERNAL_STORAGE, granted = false)
        setFakePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, granted = false)

        repository = PermissionRepository(mockContext, fakeLifecycleOwner)
        fakeLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    @AfterTest
    fun cleanup() {
        fakeLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        Dispatchers.resetMain()
    }

    @Test
    fun `permissions - initializes with current permissions`() = runTest {
        repository.permissions.value shouldBe RuntimePermission(
            canReadAudioFiles = false,
            canWriteAudioFiles = false,
        )
    }

    @Test
    fun `permissions - automatically updates when app goes foreground`() = runTest {
        setFakePermission(Manifest.permission.READ_EXTERNAL_STORAGE, granted = true)

        repository.permissions.drop(1).test {
            fakeLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            awaitItem() shouldBe RuntimePermission(
                canReadAudioFiles = true,
                canWriteAudioFiles = false
            )
        }
    }

    @Test
    fun `permissions - updates value when refreshed manually`() = runTest {
        repository.permissions.drop(1).test {
            setFakePermission(Manifest.permission.READ_EXTERNAL_STORAGE, granted = true)
            repository.refreshPermissions()
            awaitItem() shouldBe RuntimePermission(
                canReadAudioFiles = true,
                canWriteAudioFiles = false,
            )

            setFakePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, granted = true)
            repository.refreshPermissions()
            awaitItem() shouldBe RuntimePermission(
                canReadAudioFiles = true,
                canWriteAudioFiles = true,
            )
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun setFakePermission(permission: String, granted: Boolean) {
        every { mockContext.checkPermission(permission, any(), any()) } returns when (granted) {
            true -> PackageManager.PERMISSION_GRANTED
            false -> PackageManager.PERMISSION_DENIED
        }
    }
}
