/*
 * Copyright 2020 Thibault Seisel
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

package fr.nihilus.music.service.extensions

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Execute the request to load a bitmap, suspending execution until the bitmap is loaded.
 * That bitmap will be scaled to fit in the specified dimensions.
 *
 * If execution of the current coroutine is cancelled then the load request is also cancelled.
 *
 * @param width The desired width in pixels or [Target.SIZE_ORIGINAL] to use the original width.
 * This will be overridden by [RequestBuilder.override] if previously called.
 * @param height The desired height in pixels or [Target.SIZE_ORIGINAL] to use the original height.
 * This will be overridden by  [RequestBuilder.override] if previously called.
 */
internal suspend fun RequestBuilder<Bitmap>.intoBitmap(width: Int, height: Int): Bitmap? =
    suspendCancellableCoroutine { continuation ->
        val target = into(BitmapSuspendTarget(width, height, continuation))
        continuation.invokeOnCancellation {
            target.request?.clear()
        }
    }

/**
 * A target that loads a bitmap and resumes execution of a coroutine as soon as it is available.
 * In case of failure, the coroutine is resumed with a `null` bitmap instead of an exception.
 *
 * @param width The desired bitmap width in pixels.
 * @param height The desired bitmap height in pixels.
 * @param continuation Handler to resume execution of the suspending coroutine.
 */
private class BitmapSuspendTarget(
    width: Int,
    height: Int,
    private val continuation: CancellableContinuation<Bitmap?>
) : CustomTarget<Bitmap>(width, height) {

    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
        if (continuation.isActive) {
            continuation.resume(resource)
        }
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        if (continuation.isActive) {
            continuation.resume(null)
        }
    }

    override fun onLoadCleared(placeholder: Drawable?) {
        // Do nothing, as the loaded bitmap is out of this target's control.
    }
}