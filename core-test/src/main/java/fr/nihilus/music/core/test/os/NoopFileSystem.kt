/*
 * Copyright 2019 Thibault Seisel
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

package fr.nihilus.music.core.test.os

import android.graphics.Bitmap
import android.net.Uri
import fr.nihilus.music.core.os.FileSystem

/**
 * A [FileSystem] implementation that doesn't do anything useful.
 */
internal object NoopFileSystem : FileSystem {

    override fun writeBitmapToInternalStorage(filepath: String, bitmap: Bitmap): Uri? = null

    override fun makeSharedContentUri(filePath: String): Uri? = null

    override fun deleteFile(filepath: String): Boolean = true
}