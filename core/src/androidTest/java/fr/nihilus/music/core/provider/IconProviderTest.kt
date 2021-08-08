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

package fr.nihilus.music.core.provider

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.contentValuesOf
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import fr.nihilus.music.core.instrumentation.provider.ProviderTestRule
import fr.nihilus.music.core.os.PlaylistIconDir
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.longs.shouldBeExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import java.io.*
import javax.inject.Inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

private const val TEST_AUTHORITY = "fr.nihilus.music.core.test.provider"
private val BASE_PROVIDER_URI = Uri.Builder()
    .scheme(ContentResolver.SCHEME_CONTENT)
    .authority(TEST_AUTHORITY)
    .path("icons")
    .build()

@HiltAndroidTest
internal class IconProviderTest {
    private val hiltRule = HiltAndroidRule(this)
    private val providerRule: ProviderTestRule =
        ProviderTestRule.create(IconProvider::class.java, TEST_AUTHORITY)

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(hiltRule).around(providerRule)

    @Inject @PlaylistIconDir
    internal lateinit var iconsDir: File

    private lateinit var tempIconFile: File
    private lateinit var iconFileUri: Uri

    @BeforeTest
    fun setupTestFile() {
        hiltRule.inject()

        iconsDir.mkdirs()
        tempIconFile = File.createTempFile("icon-", ".png", iconsDir)
        tempIconFile.writeText("PNG")
        iconFileUri = Uri.withAppendedPath(BASE_PROVIDER_URI, tempIconFile.name)
    }

    @AfterTest
    fun cleanupTestFile() {
        check(tempIconFile.delete()) {
            "Unable to delete temp icon file at ${tempIconFile.absolutePath}"
        }
    }

    @Test
    fun query_anExistingFile_returnsOneRow() {
        val cursor = providerRule.resolver.query(iconFileUri, null, null, null, null)
        cursor.shouldNotBeNull()
        cursor.use {
            it.moveToFirst()
            it.count.shouldBeExactly(1)
            it.columnNames.shouldContainExactly(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)

            it.getString(0).shouldBe(tempIconFile.name)
            it.getLong(1).shouldBeExactly(tempIconFile.length())
        }
    }

    @Test
    fun query_anUnknownFile_returnsNullCursor() {
        val badFileUri = Uri.withAppendedPath(BASE_PROVIDER_URI, "some-icon.png")
        val cursor = providerRule.resolver.query(badFileUri, null, null, null, null)
        cursor?.close()

        cursor.shouldBeNull()
    }

    @Test
    fun query_withProjection_returnsSelectedColumns() {
        val cursor = providerRule.resolver.query(
            iconFileUri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )

        cursor.shouldNotBeNull()
        cursor.use {
            it.moveToFirst()
            it.columnNames.shouldContainExactly(OpenableColumns.DISPLAY_NAME)
            it.getString(0).shouldBe(tempIconFile.name)
        }
    }

    @Test
    fun getType_returnsMimeTypeForFile() {
        val mimeType = providerRule.resolver.getType(iconFileUri)
        mimeType.shouldBe("image/png")
    }

    @Test
    fun getType_returnsNullForInvalidUri() {
        val invalidFileUri = Uri.withAppendedPath(BASE_PROVIDER_URI, "missing-icon.png")
        val mimeType = providerRule.resolver.getType(invalidFileUri)

        mimeType.shouldBeNull()
    }

    @Test
    fun openFile_returnsReadonlyFileDescriptor() {
        val file = providerRule.resolver.openFile(iconFileUri, "r", null)
        file.shouldNotBeNull()

        file.use {
            val fileContent = FileInputStream(file.fileDescriptor).reader().readText()
            fileContent.shouldBe("PNG")

            shouldThrow<IOException> {
                FileOutputStream(it.fileDescriptor).write("Hello World!".toByteArray())
            }
        }
    }

    @Test
    fun openFile_inWriteMode_throwsSecurityException() {
        shouldThrow<SecurityException> {
            providerRule.resolver.openFile(iconFileUri, "w", null)
        }
        shouldThrow<SecurityException> {
            providerRule.resolver.openFile(iconFileUri, "rw", null)
        }
    }

    @Test
    fun openFile_withInvalidUri_throwsFileNotFoundException() {
        val invalidIconUri = Uri.withAppendedPath(BASE_PROVIDER_URI, "missing-file.png")
        shouldThrow<FileNotFoundException> {
            providerRule.resolver.openFile(invalidIconUri, "r", null)
        }
    }

    @Test
    fun insert_throwsUnsupportedOperationException() {
        shouldThrow<UnsupportedOperationException> {
            providerRule.resolver.insert(
                BASE_PROVIDER_URI, contentValuesOf(
                    OpenableColumns.DISPLAY_NAME to "new-icon.png",
                    OpenableColumns.SIZE to 42
                )
            )
        }
    }

    @Test
    fun update_throwsUnsupportedOperationException() {
        shouldThrow<UnsupportedOperationException> {
            providerRule.resolver.update(
                iconFileUri,
                contentValuesOf(OpenableColumns.DISPLAY_NAME to "new-name.png"),
                null,
                null
            )
        }
    }

    @Test
    fun delete_throwsUnsupportedOperationException() {
        shouldThrow<UnsupportedOperationException> {
            providerRule.resolver.delete(iconFileUri, null, null)
        }
    }
}
