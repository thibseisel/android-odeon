/*
 * Copyright 2018 Thibault Seisel
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

package fr.nihilus.music.glide.palette

import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.util.Util

class PaletteBitmapResource(
    private val paletteBitmap: PaletteBitmap,
    private val bitmapPool: BitmapPool
) : Resource<PaletteBitmap> {

    override fun getResourceClass() = PaletteBitmap::class.java

    override fun get() = this.paletteBitmap

    override fun getSize() = Util.getBitmapByteSize(paletteBitmap.bitmap)

    override fun recycle() = bitmapPool.put(paletteBitmap.bitmap)
}
